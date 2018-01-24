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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.ellipsis.webdav.server.ITransaction;
import nl.ellipsis.webdav.server.IWebDAVStore;
import nl.ellipsis.webdav.server.StoredObject;
import nl.ellipsis.webdav.server.WebDAVStatus;
import nl.ellipsis.webdav.server.exceptions.LockFailedException;
import nl.ellipsis.webdav.server.locking.IResourceLocks;
import nl.ellipsis.webdav.server.locking.LockedObject;

public class DoUnlock extends DeterminableMethod {

	private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DoUnlock.class);

	private IWebDAVStore _store;
	private IResourceLocks _resourceLocks;
	private boolean _readOnly;

	public DoUnlock(IWebDAVStore store, IResourceLocks resourceLocks, boolean readOnly) {
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

		if (_readOnly) {
			resp.sendError(WebDAVStatus.SC_FORBIDDEN);
			return;
		} else {
			String tempLockOwner = "doUnlock" + System.currentTimeMillis() + req.toString();
			try {
				if (_resourceLocks.lock(transaction, path, tempLockOwner, false, 0, TEMP_TIMEOUT, TEMPORARY)) {

					String lockId = getLockIdFromLockTokenHeader(req);
					LockedObject lo;
					if (lockId != null && ((lo = _resourceLocks.getLockedObjectByID(transaction, lockId)) != null)) {

						String[] owners = lo.getOwner();
						String owner = null;
						if (lo.isShared()) {
							// more than one owner is possible
							if (owners != null) {
								for (int i = 0; i < owners.length; i++) {
									// remove owner from LockedObject
									lo.removeLockedObjectOwner(owners[i]);
								}
							}
						} else {
							// exclusive, only one lock owner
							if (owners != null)
								owner = owners[0];
							else
								owner = null;
						}

						if (_resourceLocks.unlock(transaction, lockId, owner)) {
							StoredObject so = _store.getStoredObject(transaction, path);
							if (so.isNullResource()) {
								_store.removeObject(transaction, path);
							}

							resp.setStatus(WebDAVStatus.SC_NO_CONTENT);
						} else {
							LOG.debug("DoUnlock failure at " + lo.getPath());
							resp.sendError(WebDAVStatus.SC_METHOD_FAILURE);
						}
					} else {
						resp.sendError(WebDAVStatus.SC_BAD_REQUEST);
					}
				}
			} catch (LockFailedException e) {
				e.printStackTrace();
			} finally {
				_resourceLocks.unlockTemporaryLockedObjects(transaction, path, tempLockOwner);
			}
		}
	}

}
