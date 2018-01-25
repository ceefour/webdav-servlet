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

import nl.ellipsis.webdav.server.ITransaction;
import nl.ellipsis.webdav.server.IWebDAVStore;
import nl.ellipsis.webdav.server.StoredObject;
import nl.ellipsis.webdav.server.WebDAVConstants;
import nl.ellipsis.webdav.server.WebDAVStatus;
import nl.ellipsis.webdav.server.exceptions.AccessDeniedException;
import nl.ellipsis.webdav.server.exceptions.LockFailedException;
import nl.ellipsis.webdav.server.exceptions.ObjectAlreadyExistsException;
import nl.ellipsis.webdav.server.exceptions.ObjectNotFoundException;
import nl.ellipsis.webdav.server.exceptions.WebDAVException;
import nl.ellipsis.webdav.server.locking.ResourceLocks;
import nl.ellipsis.webdav.server.util.CharsetUtil;
import nl.ellipsis.webdav.server.util.RequestUtil;
import nl.ellipsis.webdav.server.util.URLUtil;

public class DoCopy extends AbstractMethod {

	private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DoCopy.class);

	private IWebDAVStore _store;
	private ResourceLocks _resourceLocks;
	private DoDelete _doDelete;
	private boolean _readOnly;

	public DoCopy(IWebDAVStore store, ResourceLocks resourceLocks, DoDelete doDelete, boolean readOnly) {
		_store = store;
		_resourceLocks = resourceLocks;
		_doDelete = doDelete;
		_readOnly = readOnly;
	}

	public void execute(ITransaction transaction, HttpServletRequest req, HttpServletResponse resp)
			throws IOException, LockFailedException {
		String path = getRelativePath(req);
		if(LOG.isDebugEnabled()) {
			LOG.debug("-- " + this.getClass().getName()+" "+path);
		}
		if (!_readOnly) {
			String tempLockOwner = "doCopy" + System.currentTimeMillis() + req.toString();
			if (_resourceLocks.lock(transaction, path, tempLockOwner, false, 0, TEMP_TIMEOUT, TEMPORARY)) {
				try {
					if (!copyResource(transaction, req, resp))
						return;
				} catch (AccessDeniedException e) {
					resp.sendError(WebDAVStatus.SC_FORBIDDEN);
				} catch (ObjectAlreadyExistsException e) {
					resp.sendError(WebDAVStatus.SC_CONFLICT, req.getRequestURI());
				} catch (ObjectNotFoundException e) {
					resp.sendError(WebDAVStatus.SC_NOT_FOUND, req.getRequestURI());
				} catch (WebDAVException e) {
					resp.sendError(WebDAVStatus.SC_INTERNAL_SERVER_ERROR);
				} finally {
					_resourceLocks.unlockTemporaryLockedObjects(transaction, path, tempLockOwner);
				}
			} else {
				resp.sendError(WebDAVStatus.SC_INTERNAL_SERVER_ERROR);
			}

		} else {
			resp.sendError(WebDAVStatus.SC_FORBIDDEN);
		}

	}

	/**
	 * Copy a resource.
	 * 
	 * @param transaction
	 *            indicates that the method is within the scope of a WebDAV
	 *            transaction
	 * @param req
	 *            Servlet request
	 * @param resp
	 *            Servlet response
	 * @return true if the copy is successful
	 * @throws WebDAVException
	 *             if an error in the underlying store occurs
	 * @throws IOException
	 *             when an error occurs while sending the response
	 * @throws LockFailedException
	 */
	public boolean copyResource(ITransaction transaction, HttpServletRequest req, HttpServletResponse resp)
			throws WebDAVException, IOException, LockFailedException {

		// Parsing destination header
		String destinationPath = parseDestinationHeader(req, resp);

		if (destinationPath == null) {
			return false;
		}

		String path = getRelativePath(req);
		
		if (path.equals(destinationPath)) {
			resp.sendError(WebDAVStatus.SC_FORBIDDEN);
			return false;
		}

		Hashtable<String, Integer> errorList = new Hashtable<String, Integer>();
		String parentPath = URLUtil.getParentPath(path);
		String parentDestinationPath = URLUtil.getParentPath(destinationPath);

		if (!checkLocks(transaction, req, resp, _resourceLocks, parentPath)) {
			resp.setStatus(WebDAVStatus.SC_LOCKED);
			return false; // parent is locked
		}

		if (!checkLocks(transaction, req, resp, _resourceLocks, parentDestinationPath)) {
			resp.setStatus(WebDAVStatus.SC_LOCKED);
			return false; // parentDestination is locked
		}

		if (!checkLocks(transaction, req, resp, _resourceLocks, destinationPath)) {
			resp.setStatus(WebDAVStatus.SC_LOCKED);
			return false; // destination is locked
		}

		// Parsing overwrite header

		boolean overwrite = true;
		String overwriteHeader = req.getHeader(WebDAVConstants.HttpHeader.OVERWRITE);

		if (overwriteHeader != null) {
			overwrite = overwriteHeader.equalsIgnoreCase("T");
		}

		// Overwriting the destination
		String lockOwner = "copyResource" + System.currentTimeMillis() + req.toString();

		if (_resourceLocks.lock(transaction, destinationPath, lockOwner, false, 0, TEMP_TIMEOUT, TEMPORARY)) {
			StoredObject copySo, destinationSo = null;
			try {
				copySo = _store.getStoredObject(transaction, path);
				// Retrieve the resources
				if (copySo == null) {
					resp.sendError(HttpServletResponse.SC_NOT_FOUND);
					return false;
				}

				if (copySo.isNullResource()) {
					String methodsAllowed = DeterminableMethod.determineMethodsAllowed(copySo);
					resp.addHeader(WebDAVConstants.HttpHeader.ALLOW, methodsAllowed);
					resp.sendError(WebDAVStatus.SC_METHOD_NOT_ALLOWED);
					return false;
				}

				errorList = new Hashtable<String, Integer>();

				destinationSo = _store.getStoredObject(transaction, destinationPath);

				if (overwrite) {
					// Delete destination resource, if it exists
					if (destinationSo != null) {
						_doDelete.deleteResource(transaction, destinationPath, errorList, req, resp);
					} else {
						resp.setStatus(WebDAVStatus.SC_CREATED);
					}
				} else {
					// If the destination exists, then it's a conflict
					if (destinationSo != null) {
						resp.sendError(WebDAVStatus.SC_PRECONDITION_FAILED);
						return false;
					} else {
						resp.setStatus(WebDAVStatus.SC_CREATED);
					}
				}
				copy(transaction, path, destinationPath, errorList, req, resp);

				if (!errorList.isEmpty()) {
					sendReport(req, resp, errorList);
				}
			} finally {
				_resourceLocks.unlockTemporaryLockedObjects(transaction, destinationPath, lockOwner);
			}
		} else {
			resp.sendError(WebDAVStatus.SC_INTERNAL_SERVER_ERROR);
			return false;
		}
		return true;

	}

	/**
	 * copies the specified resource(s) to the specified destination. preconditions
	 * must be handled by the caller. Standard status codes must be handled by the
	 * caller. a multi status report in case of errors is created here.
	 * 
	 * @param transaction
	 *            indicates that the method is within the scope of a WebDAV
	 *            transaction
	 * @param sourcePath
	 *            path from where to read
	 * @param destinationPath
	 *            path where to write
	 * @param req
	 *            HttpServletRequest
	 * @param resp
	 *            HttpServletResponse
	 * @throws WebDAVException
	 *             if an error in the underlying store occurs
	 * @throws IOException
	 */
	private void copy(ITransaction transaction, String sourcePath, String destinationPath,
			Hashtable<String, Integer> errorList, HttpServletRequest req, HttpServletResponse resp)
			throws WebDAVException, IOException {

		StoredObject sourceSo = _store.getStoredObject(transaction, sourcePath);
		if (sourceSo.isResource()) {
			_store.createResource(transaction, destinationPath);
			long resourceLength = _store.setResourceContent(transaction, destinationPath,
					_store.getResourceContent(transaction, sourcePath), null, null);

			if (resourceLength != -1) {
				StoredObject destinationSo = _store.getStoredObject(transaction, destinationPath);
				destinationSo.setResourceLength(resourceLength);
			}
		} else {
			if (sourceSo.isFolder()) {
				copyFolder(transaction, sourcePath, destinationPath, errorList, req, resp);
			} else {
				resp.sendError(WebDAVStatus.SC_NOT_FOUND);
			}
		}
	}

	/**
	 * helper method of copy() recursively copies the FOLDER at source path to
	 * destination path
	 * 
	 * @param transaction
	 *            indicates that the method is within the scope of a WebDAV
	 *            transaction
	 * @param sourcePath
	 *            where to read
	 * @param destinationPath
	 *            where to write
	 * @param errorList
	 *            all errors that ocurred
	 * @param req
	 *            HttpServletRequest
	 * @param resp
	 *            HttpServletResponse
	 * @throws WebDAVException
	 *             if an error in the underlying store occurs
	 */
	private void copyFolder(ITransaction transaction, String sourcePath, String destinationPath,
			Hashtable<String, Integer> errorList, HttpServletRequest req, HttpServletResponse resp)
			throws WebDAVException {

		_store.createFolder(transaction, destinationPath);
		boolean infiniteDepth = true;
		String depth = req.getHeader(WebDAVConstants.HttpHeader.DEPTH);
		if (depth != null) {
			if (depth.equals("0")) {
				infiniteDepth = false;
			}
		}
		if (infiniteDepth) {
			String[] children = _store.getChildrenNames(transaction, sourcePath);
			children = children == null ? new String[] {} : children;

			StoredObject childSo;
			for (int i = children.length - 1; i >= 0; i--) {
				String childSourcePath = URLUtil.getCleanPath(sourcePath,  children[i]);
				String destinationSourcePath = URLUtil.getCleanPath(destinationPath,  children[i]);
				try {
					childSo = _store.getStoredObject(transaction, childSourcePath);
					if (childSo.isResource()) {
						_store.createResource(transaction, destinationSourcePath);
						long resourceLength = _store.setResourceContent(transaction,destinationSourcePath,
								_store.getResourceContent(transaction, childSourcePath), null, null);

						if (resourceLength != -1) {
							StoredObject destinationSo = _store.getStoredObject(transaction,destinationSourcePath);
							destinationSo.setResourceLength(resourceLength);
						}
					} else {
						copyFolder(transaction, childSourcePath, destinationSourcePath, errorList, req, resp);
					}
				} catch (AccessDeniedException e) {
					errorList.put(destinationSourcePath, new Integer(WebDAVStatus.SC_FORBIDDEN));
				} catch (ObjectNotFoundException e) {
					errorList.put(destinationSourcePath, new Integer(WebDAVStatus.SC_NOT_FOUND));
				} catch (ObjectAlreadyExistsException e) {
					errorList.put(destinationSourcePath, new Integer(WebDAVStatus.SC_CONFLICT));
				} catch (WebDAVException e) {
					errorList.put(destinationSourcePath, new Integer(WebDAVStatus.SC_INTERNAL_SERVER_ERROR));
				}
			}
		}
	}

	/**
	 * Parses and normalizes the destination header.
	 * 
	 * @param req
	 *            Servlet request
	 * @param resp
	 *            Servlet response
	 * @return destinationPath
	 * @throws IOException
	 *             if an error occurs while sending response
	 */
	private String parseDestinationHeader(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		String destinationPath = req.getHeader(WebDAVConstants.HttpHeader.DESTINATION);

		if (destinationPath == null) {
			resp.sendError(WebDAVStatus.SC_BAD_REQUEST);
			return null;
		}

		// Remove url encoding from destination
		destinationPath = RequestUtil.URLDecode(destinationPath, "UTF8");

		int protocolIndex = destinationPath.indexOf(CharsetUtil.COLON+CharsetUtil.FORWARD_SLASH+CharsetUtil.FORWARD_SLASH);
		if (protocolIndex >= 0) {
			// if the Destination URL contains the protocol, we can safely
			// trim everything upto the first "/" character after "://"
			int firstSeparator = destinationPath.indexOf(CharsetUtil.CHAR_FORWARD_SLASH, protocolIndex + 4);
			if (firstSeparator < 0) {
				destinationPath = CharsetUtil.FORWARD_SLASH;
			} else {
				destinationPath = destinationPath.substring(firstSeparator);
			}
		} else {
			String hostName = req.getServerName();
			if ((hostName != null) && (destinationPath.startsWith(hostName))) {
				destinationPath = destinationPath.substring(hostName.length());
			}

			int portIndex = destinationPath.indexOf(CharsetUtil.CHAR_COLON);
			if (portIndex >= 0) {
				destinationPath = destinationPath.substring(portIndex);
			}

			if (destinationPath.startsWith(CharsetUtil.COLON)) {
				int firstSeparator = destinationPath.indexOf(CharsetUtil.CHAR_FORWARD_SLASH);
				if (firstSeparator < 0) {
					destinationPath = CharsetUtil.FORWARD_SLASH;
				} else {
					destinationPath = destinationPath.substring(firstSeparator);
				}
			}
		}

		// Normalize destination path (remove '.' and '..')
		destinationPath = URLUtil.normalize(destinationPath);

		String contextPath = req.getContextPath();
		if ((contextPath != null) && (destinationPath.startsWith(contextPath))) {
			destinationPath = destinationPath.substring(contextPath.length());
		}

		String pathInfo = req.getPathInfo();
		if (pathInfo != null) {
			String servletPath = req.getServletPath();
			if ((servletPath != null) && (destinationPath.startsWith(servletPath))) {
				destinationPath = destinationPath.substring(servletPath.length());
			}
		}

		return destinationPath;
	}



}
