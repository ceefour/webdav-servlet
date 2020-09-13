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

import nl.ellipsis.webdav.HttpHeaders;
import nl.ellipsis.webdav.server.ITransaction;
import nl.ellipsis.webdav.server.WebDAVConstants;
import nl.ellipsis.webdav.server.exceptions.AccessDeniedException;
import nl.ellipsis.webdav.server.exceptions.LockFailedException;
import nl.ellipsis.webdav.server.exceptions.ObjectAlreadyExistsException;
import nl.ellipsis.webdav.server.exceptions.WebDAVException;
import nl.ellipsis.webdav.server.locking.ResourceLocks;

public class DoMove extends AbstractMethod {

	private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DoMove.class);

	private ResourceLocks _resourceLocks;
	private DoDelete _doDelete;
	private DoCopy _doCopy;
	private boolean _readOnly;

	public DoMove(ResourceLocks resourceLocks, DoDelete doDelete, DoCopy doCopy, boolean readOnly) {
		_resourceLocks = resourceLocks;
		_doDelete = doDelete;
		_doCopy = doCopy;
		_readOnly = readOnly;
	}

	public void execute(ITransaction transaction, HttpServletRequest req, HttpServletResponse resp)
			throws IOException, LockFailedException {
		String sourcePath = getRelativePath(req);
		String destinationPath = req.getHeader(HttpHeaders.DESTINATION);
		if(LOG.isDebugEnabled()) {
			LOG.debug("-- " + this.getClass().getName()+" "+sourcePath+" -> "+destinationPath);
		}

		if (!_readOnly) {
			Hashtable<String, Integer> errorList = new Hashtable<String, Integer>();

			if (!checkLocks(transaction, req, resp, _resourceLocks, sourcePath)) {
				resp.setStatus(HttpStatus.LOCKED.value());
				return;
			}

			if (destinationPath == null) {
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}

			if (!checkLocks(transaction, req, resp, _resourceLocks, destinationPath)) {
				resp.setStatus(HttpStatus.LOCKED.value());
				return;
			}

			String tempLockOwner = "doMove" + System.currentTimeMillis() + req.toString();

			if (_resourceLocks.lock(transaction, sourcePath, tempLockOwner, false, 0, TEMP_TIMEOUT, TEMPORARY)) {
				try {

					if (_doCopy.copyResource(transaction, req, resp)) {

						errorList = new Hashtable<String, Integer>();
						_doDelete.deleteResource(transaction, sourcePath, errorList, req, resp);
						if (!errorList.isEmpty()) {
							sendReport(req, resp, errorList);
						}
					}

				} catch (AccessDeniedException e) {
					resp.sendError(HttpServletResponse.SC_FORBIDDEN);
				} catch (ObjectAlreadyExistsException e) {
					resp.sendError(HttpServletResponse.SC_NOT_FOUND, req.getRequestURI());
				} catch (WebDAVException e) {
					resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				} finally {
					_resourceLocks.unlockTemporaryLockedObjects(transaction, sourcePath, tempLockOwner);
				}
			} else {
				errorList.put(req.getHeader(HttpHeaders.DESTINATION), HttpStatus.LOCKED.value());
				sendReport(req, resp, errorList);
			}
		} else {
			resp.sendError(HttpServletResponse.SC_FORBIDDEN);
		}
	}

}
