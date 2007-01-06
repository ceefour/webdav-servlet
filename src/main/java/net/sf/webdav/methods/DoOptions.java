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
package net.sf.webdav.methods;

import net.sf.webdav.WebdavStore;
import net.sf.webdav.ResourceLocks;
import net.sf.webdav.WebdavStatus;
import net.sf.webdav.exceptions.AccessDeniedException;
import net.sf.webdav.exceptions.WebdavException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class DoOptions extends DeterminableMethod {

    private WebdavStore store;
    private ResourceLocks resLocks;
    private int debug;

    public DoOptions(WebdavStore store, ResourceLocks resLocks, int debug) {
        this.store = store;
        this.resLocks = resLocks;
        this.debug = debug;
    }

    public void execute(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        if (debug == 1)
            System.err.println("-- doOptions");

        String lockOwner = "doOptions" + System.currentTimeMillis()
                + req.toString();
        String path = getRelativePath(req);
        if (resLocks.lock(path, lockOwner, false, 0)) {
            try {
                resp.addHeader("DAV", "1, 2");

                String methodsAllowed = determineMethodsAllowed(store
                        .objectExists(path), store.isFolder(path));
                resp.addHeader("Allow", methodsAllowed);
                resp.addHeader("MS-Author-Via", "DAV");
            } catch (AccessDeniedException e) {
                resp.sendError(WebdavStatus.SC_FORBIDDEN);
            } catch (WebdavException e) {
                resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
            } finally {
                resLocks.unlock(path, lockOwner);
            }
        } else {
            resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
        }    }
}
