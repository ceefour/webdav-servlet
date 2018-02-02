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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.ellipsis.webdav.server.methods;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.ellipsis.webdav.server.IMimeTyper;
import nl.ellipsis.webdav.server.ITransaction;
import nl.ellipsis.webdav.server.IWebDAVStore;
import nl.ellipsis.webdav.server.StoredObject;
import nl.ellipsis.webdav.server.WebDAVConstants;
import nl.ellipsis.webdav.server.exceptions.AccessDeniedException;
import nl.ellipsis.webdav.server.exceptions.LockFailedException;
import nl.ellipsis.webdav.server.exceptions.ObjectAlreadyExistsException;
import nl.ellipsis.webdav.server.exceptions.WebDAVException;
import nl.ellipsis.webdav.server.locking.ResourceLocks;
import nl.ellipsis.webdav.server.util.CharsetUtil;
import nl.ellipsis.webdav.server.util.URLUtil;

public class DoHead extends AbstractMethod {

	protected String _dftIndexFile;
	protected IWebDAVStore _store;
	protected String _insteadOf404;
	protected ResourceLocks _resourceLocks;
	protected IMimeTyper _mimeTyper;
	protected int _contentLength;

	private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DoHead.class);

	public DoHead(IWebDAVStore store, String dftIndexFile, String insteadOf404, ResourceLocks resourceLocks,
			IMimeTyper mimeTyper, int contentLengthHeader) {
		_store = store;
		_dftIndexFile = dftIndexFile;
		_insteadOf404 = insteadOf404;
		_resourceLocks = resourceLocks;
		_mimeTyper = mimeTyper;
		_contentLength = contentLengthHeader;
	}

	public void execute(ITransaction transaction, HttpServletRequest req, HttpServletResponse resp)
			throws IOException, LockFailedException {
		String path = getRelativePath(req);
		if(LOG.isDebugEnabled()) {
			LOG.debug("-- " + this.getClass().getName()+" "+path);
		}
		// determines if the uri exists.
		boolean bUriExists = false;

		StoredObject so;
		try {
			so = _store.getStoredObject(transaction, path);
			if (so == null) {
				if (this._insteadOf404 != null && !_insteadOf404.trim().equals("")) {
					path = this._insteadOf404;
					so = _store.getStoredObject(transaction, this._insteadOf404);
				}
			} else
				bUriExists = true;
		} catch (AccessDeniedException e) {
			resp.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

		if (so != null) {
			if (so.isFolder()) {
				if (_dftIndexFile != null && !_dftIndexFile.trim().equals("")) {
					resp.sendRedirect(resp.encodeRedirectURL(URLUtil.getCleanPath(req.getRequestURI(),this._dftIndexFile)));
					return;
				}
			} else if (so.isNullResource()) {
				String methodsAllowed = DeterminableMethod.determineMethodsAllowed(so);
				resp.addHeader(javax.ws.rs.core.HttpHeaders.ALLOW, methodsAllowed);
				resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
				return;
			}

			String tempLockOwner = "doGet" + System.currentTimeMillis() + req.toString();

			if (_resourceLocks.lock(transaction, path, tempLockOwner, false, 0, TEMP_TIMEOUT, TEMPORARY)) {
				try {

					String eTagMatch = req.getHeader(javax.ws.rs.core.HttpHeaders.IF_NONE_MATCH);
					if (eTagMatch != null) {
						if (eTagMatch.equals(getETag(so))) {
							resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
							return;
						}
					}

					if (so.isResource()) {
						// path points to a file but ends with / or \
						if (path.endsWith(CharsetUtil.FORWARD_SLASH) || (path.endsWith(CharsetUtil.BACKSLASH))) {
							resp.sendError(HttpServletResponse.SC_NOT_FOUND, req.getRequestURI());
						} else {

							// setting headers
							long lastModified = so.getLastModified().getTime();
							resp.setDateHeader("last-modified", lastModified);

							String eTag = getETag(so);
							resp.addHeader(javax.ws.rs.core.HttpHeaders.ETAG, eTag);

							long resourceLength = so.getResourceLength();

							if (_contentLength == 1) {
								if (resourceLength > 0) {
									if (resourceLength <= Integer.MAX_VALUE) {
										resp.setContentLength((int) resourceLength);
									} else {
										resp.setHeader(javax.ws.rs.core.HttpHeaders.CONTENT_LENGTH, Long.toString(resourceLength));
										// is "content-length" the right header?
										// is long a valid format?
									}
								}
							}

							String mimeType = _mimeTyper.getMimeType(transaction, path);
							if (mimeType != null) {
								resp.setContentType(mimeType);
							} else {
								int lastSlash = path.replace(CharsetUtil.CHAR_BACKSLASH, CharsetUtil.CHAR_FORWARD_SLASH).lastIndexOf(CharsetUtil.CHAR_FORWARD_SLASH);
								int lastDot = path.indexOf(CharsetUtil.CHAR_DOT, lastSlash);
								if (lastDot == -1) {
									resp.setContentType("text/html");
								}
							}
							doBody(transaction, resp, path);
						}
					} else {
						folderBody(transaction, path, resp, req);
					}
				} catch (AccessDeniedException e) {
					resp.sendError(HttpServletResponse.SC_FORBIDDEN);
				} catch (ObjectAlreadyExistsException e) {
					resp.sendError(HttpServletResponse.SC_NOT_FOUND, req.getRequestURI());
				} catch (WebDAVException e) {
					resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				} finally {
					_resourceLocks.unlockTemporaryLockedObjects(transaction, path, tempLockOwner);
				}
			} else {
				resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			}
		} else {
			folderBody(transaction, path, resp, req);
		}

		if (!bUriExists)
			resp.setStatus(HttpServletResponse.SC_NOT_FOUND);

	}

	protected void folderBody(ITransaction transaction, String path, HttpServletResponse resp, HttpServletRequest req)
			throws IOException {
		// no body for HEAD
	}

	protected void doBody(ITransaction transaction, HttpServletResponse resp, String path) throws IOException {
		// no body for HEAD
	}
}
