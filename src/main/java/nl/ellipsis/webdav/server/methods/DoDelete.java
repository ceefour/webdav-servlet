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
import nl.ellipsis.webdav.server.exceptions.AccessDeniedException;
import nl.ellipsis.webdav.server.exceptions.LockFailedException;
import nl.ellipsis.webdav.server.exceptions.ObjectAlreadyExistsException;
import nl.ellipsis.webdav.server.exceptions.ObjectNotFoundException;
import nl.ellipsis.webdav.server.exceptions.WebDAVException;
import nl.ellipsis.webdav.server.locking.ResourceLocks;
import nl.ellipsis.webdav.server.util.URLUtil;

public class DoDelete extends AbstractMethod {

	private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DoDelete.class);

	private IWebDAVStore _store;
	private ResourceLocks _resourceLocks;
	private boolean _readOnly;

	public DoDelete(IWebDAVStore store, ResourceLocks resourceLocks, boolean readOnly) {
		_store = store;
		_resourceLocks = resourceLocks;
		_readOnly = readOnly;
	}

	public void execute(ITransaction transaction, HttpServletRequest req, HttpServletResponse resp)
			throws IOException, LockFailedException {
		String path = getRelativePath(req);
		if(LOG.isDebugEnabled()) {
			LOG.debug("-- " + this.getClass().getName()+" "+path);
		}
		if (!_readOnly) {
			String parentPath = URLUtil.getParentPath(path);

			Hashtable<String, Integer> errorList = new Hashtable<String, Integer>();

			if (!checkLocks(transaction, req, resp, _resourceLocks, parentPath)) {
				resp.setStatus(HttpStatus.LOCKED.value());
				return; // parent is locked
			}

			if (!checkLocks(transaction, req, resp, _resourceLocks, path)) {
				resp.setStatus(HttpStatus.LOCKED.value());
				return; // resource is locked
			}

			String tempLockOwner = "doDelete" + System.currentTimeMillis() + req.toString();
			if (_resourceLocks.lock(transaction, path, tempLockOwner, false, 0, TEMP_TIMEOUT, TEMPORARY)) {
				try {
					errorList = new Hashtable<String, Integer>();
					deleteResource(transaction, path, errorList, req, resp);
					if (!errorList.isEmpty()) {
						sendReport(req, resp, errorList);
					}
				} catch (AccessDeniedException e) {
					resp.sendError(HttpServletResponse.SC_FORBIDDEN);
				} catch (ObjectAlreadyExistsException e) {
					resp.sendError(HttpServletResponse.SC_NOT_FOUND, req.getRequestURI());
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
	 * deletes the recources at "path"
	 * 
	 * @param transaction
	 *            indicates that the method is within the scope of a WebDAV
	 *            transaction
	 * @param path
	 *            the folder to be deleted
	 * @param errorList
	 *            all errors that ocurred
	 * @param req
	 *            HttpServletRequest
	 * @param resp
	 *            HttpServletResponse
	 * @throws WebDAVException
	 *             if an error in the underlying store occurs
	 * @throws IOException
	 *             when an error occurs while sending the response
	 */
	public void deleteResource(ITransaction transaction, String path, Hashtable<String, Integer> errorList,
			HttpServletRequest req, HttpServletResponse resp) throws IOException, WebDAVException {

		resp.setStatus(HttpServletResponse.SC_NO_CONTENT);

		if (!_readOnly) {
			StoredObject so = _store.getStoredObject(transaction, path);
			if (so != null) {
				if (so.isResource()) {
					_store.removeObject(transaction, path);
				} else {
					if (so.isFolder()) {
						deleteFolder(transaction, path, errorList, req, resp);
						_store.removeObject(transaction, path);
					} else {
						resp.sendError(HttpServletResponse.SC_NOT_FOUND);
					}
				}
			} else {
				resp.sendError(HttpServletResponse.SC_NOT_FOUND);
			}
			so = null;
		} else {
			resp.sendError(HttpServletResponse.SC_FORBIDDEN);
		}
	}

	/**
	 * 
	 * helper method of deleteResource() deletes the folder and all of its contents
	 * 
	 * @param transaction
	 *            indicates that the method is within the scope of a WebDAV
	 *            transaction
	 * @param path
	 *            the folder to be deleted
	 * @param errorList
	 *            all errors that ocurred
	 * @param req
	 *            HttpServletRequest
	 * @param resp
	 *            HttpServletResponse
	 * @throws WebDAVException
	 *             if an error in the underlying store occurs
	 */
	private void deleteFolder(ITransaction transaction, String path, Hashtable<String, Integer> errorList,
			HttpServletRequest req, HttpServletResponse resp) throws WebDAVException {

		String[] children = _store.getChildrenNames(transaction, path);
		children = children == null ? new String[] {} : children;
		StoredObject so = null;
		for (int i = children.length - 1; i >= 0; i--) {
			String childPath = URLUtil.getCleanPath(path, children[i]);
			try {
				so = _store.getStoredObject(transaction, childPath);
				if (so.isResource()) {
					_store.removeObject(transaction, childPath);
				} else {
					deleteFolder(transaction, childPath, errorList, req, resp);
					_store.removeObject(transaction, childPath);
				}
			} catch (AccessDeniedException e) {
				errorList.put(path + children[i], new Integer(HttpServletResponse.SC_FORBIDDEN));
			} catch (ObjectNotFoundException e) {
				errorList.put(path + children[i], new Integer(HttpServletResponse.SC_NOT_FOUND));
			} catch (WebDAVException e) {
				errorList.put(path + children[i], new Integer(HttpServletResponse.SC_INTERNAL_SERVER_ERROR));
			}
		}
		so = null;
	}

}
