package net.sf.webdav.methods;

import net.sf.webdav.exceptions.AccessDeniedException;
import net.sf.webdav.exceptions.WebdavException;
import net.sf.webdav.WebdavStatus;
import net.sf.webdav.WebdavStore;
import net.sf.webdav.ResourceLocks;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class DoPut extends AbstractMethod {

    private WebdavStore store;
    private ResourceLocks resLocks;
    private boolean readOnly;
    private int debug;
    private boolean lazyFolderCreationOnPut;

    public DoPut(WebdavStore store, ResourceLocks resLocks, boolean readOnly, int debug, boolean lazyFolderCreationOnPut) {
        this.store = store;
        this.resLocks = resLocks;
        this.readOnly = readOnly;
        this.debug = debug;
        this.lazyFolderCreationOnPut = lazyFolderCreationOnPut;
    }


    public void execute(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (debug == 1)
            System.err.println("-- doPut");

        if (!readOnly) {
            String path = AbstractMethod.getRelativePath(req);
            String parentPath = getParentPath(path);
            String lockOwner = "doPut" + System.currentTimeMillis()
                    + req.toString();
            if (resLocks.lock(path, lockOwner, true, -1)) {
                try {
                    if (parentPath != null && !store.isFolder(parentPath)
                            && lazyFolderCreationOnPut) {
                        store.createFolder(parentPath);
                    }
                    if (!store.isFolder(path)) {
                        if (!store.objectExists(path)) {
                            store.createResource(path);
                            resp.setStatus(HttpServletResponse.SC_CREATED);
                        } else {
                            resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
                        }
                        store.setResourceContent(path, req.getInputStream(),
                                null, null);
                        resp.setContentLength((int) store
                                .getResourceLength(path));
                    }
                } catch (AccessDeniedException e) {
                    resp.sendError(WebdavStatus.SC_FORBIDDEN);
                } catch (WebdavException e) {
                    resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
                } finally {
                    resLocks.unlock(path, lockOwner);
                }
            } else {
                resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
            }
        } else {
            resp.sendError(WebdavStatus.SC_FORBIDDEN);
        }

    }
}
