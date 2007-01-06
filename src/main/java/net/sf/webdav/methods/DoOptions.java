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
        String path = AbstractMethod.getRelativePath(req);
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
