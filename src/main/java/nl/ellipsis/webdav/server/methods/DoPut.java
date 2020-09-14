/*
 * Copyright 1999,2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.ellipsis.webdav.server.methods;

import java.io.IOException;
import java.util.Hashtable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;

import nl.ellipsis.webdav.server.ITransaction;
import nl.ellipsis.webdav.server.IWebDAVStore;
import nl.ellipsis.webdav.server.StoredObject;
import nl.ellipsis.webdav.server.WebDAVConstants;
import nl.ellipsis.webdav.server.exceptions.AccessDeniedException;
import nl.ellipsis.webdav.server.exceptions.LockFailedException;
import nl.ellipsis.webdav.server.exceptions.WebDAVException;
import nl.ellipsis.webdav.server.locking.IResourceLocks;
import nl.ellipsis.webdav.server.locking.LockedObject;
import nl.ellipsis.webdav.server.util.URLUtil;

public class DoPut extends AbstractMethod {

	private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DoPut.class);

	private IWebDAVStore _store;
	private IResourceLocks _resourceLocks;
	private boolean _readOnly;
	private boolean _lazyFolderCreationOnPut;

	private String _userAgent;

	public DoPut(IWebDAVStore store, IResourceLocks resLocks, boolean readOnly, boolean lazyFolderCreationOnPut) {
		_store = store;
		_resourceLocks = resLocks;
		_readOnly = readOnly;
		_lazyFolderCreationOnPut = lazyFolderCreationOnPut;
	}

	public void execute(ITransaction transaction, HttpServletRequest req, HttpServletResponse resp)
			throws IOException, LockFailedException {
		String path = getRelativePath(req);
		if(LOG.isDebugEnabled()) {
			LOG.debug("-- " + this.getClass().getName()+" "+path);
		}

		if (!_readOnly) {
			String parentPath = URLUtil.getParentPath(path);

			_userAgent = req.getHeader(javax.ws.rs.core.HttpHeaders.USER_AGENT);

			Hashtable<String, Integer> errorList = new Hashtable<String, Integer>();

			if (!checkLocks(transaction, req, resp, _resourceLocks, parentPath)) {
				resp.setStatus(HttpStatus.LOCKED.value());
				return; // parent is locked
			}

			if (!checkLocks(transaction, req, resp, _resourceLocks, path)) {
				resp.setStatus(HttpStatus.LOCKED.value());
				return; // resource is locked
			}

			String tempLockOwner = "doPut" + System.currentTimeMillis() + req.toString();
			if (_resourceLocks.lock(transaction, path, tempLockOwner, false, 0, AbstractMethod.getTempTimeout(), TEMPORARY)) {
				StoredObject parentSo, so = null;
				try {
					parentSo = _store.getStoredObject(transaction, parentPath);
					if (parentPath != null && parentSo != null && parentSo.isResource()) {
						resp.sendError(HttpServletResponse.SC_FORBIDDEN);
						return;
					} else if (parentPath != null && parentSo == null && _lazyFolderCreationOnPut) {
						_store.createFolder(transaction, parentPath);
					} else if (parentPath != null && parentSo == null && !_lazyFolderCreationOnPut) {
						// https://tools.ietf.org/html/rfc4918#page-50 
						// A PUT that would result in the creation of a resource without an appropriately scoped parent collection MUST fail with a 409 (Conflict).
						errorList.put(parentPath, HttpServletResponse.SC_CONFLICT);
						sendReport(req, resp, errorList);
						return;
					}

					so = _store.getStoredObject(transaction, path);

					if (so == null) {
						_store.createResource(transaction, path);
						// resp.setStatus(HttpServletResponse.SC_CREATED);
					} else {
						// This has already been created, just update the data
						if (so.isNullResource()) {

							LockedObject nullResourceLo = _resourceLocks.getLockedObjectByPath(transaction, path);
							if (nullResourceLo == null) {
								LOG.error("Sending internal error - Failed to lock");
								resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
								return;
							}
							String nullResourceLockToken = nullResourceLo.getID();
							String[] lockTokens = getLockIdFromIfHeader(req);
							String lockToken = null;
							if (lockTokens != null) {
								lockToken = lockTokens[0];
							} else {
								resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
								return;
							}
							if (lockToken.equals(nullResourceLockToken)) {
								so.setNullResource(false);
								so.setFolder(false);

								String[] nullResourceLockOwners = nullResourceLo.getOwner();
								String owner = null;
								if (nullResourceLockOwners != null)
									owner = nullResourceLockOwners[0];

								if (!_resourceLocks.unlock(transaction, lockToken, owner)) {
									LOG.error("Sending internal error - Failed to unlock");
									resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
								}
							} else {
								errorList.put(path, HttpStatus.LOCKED.value());
								sendReport(req, resp, errorList);
							}
						}
					}
					// User-Agent workarounds
					doUserAgentWorkaround(resp);

					// setting resourceContent
					long resourceLength = _store.setResourceContent(transaction, path, req.getInputStream(), null, null);

					so = _store.getStoredObject(transaction, path);
					if (resourceLength != -1) {
						so.setResourceLength(resourceLength);
					}
					// Now lets report back what was actually saved
				} catch (AccessDeniedException e) {
					resp.sendError(HttpServletResponse.SC_FORBIDDEN);
				} catch (WebDAVException e) {
					LOG.error("Sending internal error!", e);
					resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				} finally {
					_resourceLocks.unlockTemporaryLockedObjects(transaction, path, tempLockOwner);
				}
			} else {
				LOG.error("Sending internal error - Failed to lock");
				resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			}
		} else {
			resp.sendError(HttpServletResponse.SC_FORBIDDEN);
		}
	}

	/**
	 * @param resp
	 */
	private void doUserAgentWorkaround(HttpServletResponse resp) {
		if (_userAgent != null && _userAgent.indexOf("WebDAVFS") != -1 && _userAgent.indexOf("Transmit") == -1) {
			LOG.debug("DoPut.execute() : do workaround for user agent '" + _userAgent + "'");
			resp.setStatus(HttpServletResponse.SC_CREATED);
		} else if (_userAgent != null && _userAgent.indexOf("Transmit") != -1) {
			// Transmit also uses WEBDAVFS 1.x.x but crashes
			// with SC_CREATED response
			LOG.debug("DoPut.execute() : do workaround for user agent '" + _userAgent + "'");
			resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
		} else {
			resp.setStatus(HttpServletResponse.SC_CREATED);
		}
	}
}
