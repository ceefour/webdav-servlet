package net.sf.webdav.methods;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.webdav.ResourceLocks;
import net.sf.webdav.WebdavStatus;
import net.sf.webdav.WebdavStore;
import net.sf.webdav.exceptions.AccessDeniedException;
import net.sf.webdav.exceptions.ObjectAlreadyExistsException;
import net.sf.webdav.exceptions.WebdavException;

public class DoGet extends AbstractMethod {

    private int debug;

    private String dftIndexFile;

    private WebdavStore store;

    private String insteadOf404;

    private ResourceLocks resLocks;

    private int contLength;

    public DoGet(WebdavStore store, String dftIndexFile, String insteadOf404,
            ResourceLocks resourceLocks, int contentLengthHeader, int debug) {
        this.store = store;
        this.dftIndexFile = dftIndexFile;
        this.insteadOf404 = insteadOf404;
        this.resLocks = resourceLocks;
        this.contLength = contentLengthHeader;
        this.debug = debug;

    }

    public void execute(HttpServletRequest req, HttpServletResponse resp,
            boolean includeBody, String mimeType) throws IOException {
        String path = getRelativePath(req);
        if (debug == 1)
            System.err.println("-- doGet " + path);

        if (store.isFolder(path)) {
            if (dftIndexFile != null && !dftIndexFile.trim().equals("")) {
                resp.sendRedirect(resp.encodeRedirectURL(req.getRequestURI()
                        + this.dftIndexFile));
                return;
            }
        }
        if (!store.objectExists(path)) {
            if (this.insteadOf404 != null) {
                path = this.insteadOf404;
            }
        }

        String lockOwner = "doGet" + System.currentTimeMillis()
                + req.toString();

        if (resLocks.lock(path, lockOwner, false, 0)) {
            try {

                if (store.isResource(path)) {
                    // path points to a file but ends with / or \
                    if (path.endsWith("/") || (path.endsWith("\\"))) {
                        resp.sendError(HttpServletResponse.SC_NOT_FOUND, req
                                .getRequestURI());
                    } else {

                        // setting headers
                        long lastModified = store.getLastModified(path)
                                .getTime();
                        resp.setDateHeader("last-modified", lastModified);

                        long resourceLength = store.getResourceLength(path);

                        if (contLength == 1) {
                            if (resourceLength > 0) {
                                if (resourceLength <= Integer.MAX_VALUE) {
                                    resp.setContentLength((int) resourceLength);
                                } else {
                                    resp.setHeader("content-length", ""
                                            + resourceLength);
                                    // is "content-length" the right header?
                                    // is
                                    // long
                                    // a valid format?
                                }
                            }
                        }

                        if (mimeType != null) {
                            resp.setContentType(mimeType);
                        } else {
                            int lastSlash = path.replace('\\', '/')
                                    .lastIndexOf('/');
                            int lastDot = path.indexOf(".", lastSlash);
                            if (lastDot == -1) {
                                resp.setContentType("text/html");
                            }
                        }

                        if (includeBody) {
                            OutputStream out = resp.getOutputStream();
                            InputStream in = store.getResourceContent(path);
                            try {
                                int read = -1;
                                byte[] copyBuffer = new byte[BUF_SIZE];

                                while ((read = in.read(copyBuffer, 0,
                                        copyBuffer.length)) != -1) {
                                    out.write(copyBuffer, 0, read);
                                }

                            } finally {

                                in.close();
                                out.flush();
                                out.close();
                            }
                        }
                    }
                } else {
                    if (includeBody && store.isFolder(path)) {
                        // TODO some folder response (for browsers, DAV tools
                        // use propfind) in html?
                        OutputStream out = resp.getOutputStream();
                        String[] children = store.getChildrenNames(path);
                        StringBuffer childrenTemp = new StringBuffer();
                        childrenTemp.append("Contents of this Folder:\n");
                        for (int i = 0; i < children.length; i++) {
                            childrenTemp.append(children[i]);
                            childrenTemp.append("\n");
                        }
                        out.write(childrenTemp.toString().getBytes());
                    } else {
                        if (!store.objectExists(path)) {
                            resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                                    req.getRequestURI());
                        }

                    }
                }
            } catch (AccessDeniedException e) {
                resp.sendError(WebdavStatus.SC_FORBIDDEN);
            } catch (ObjectAlreadyExistsException e) {
                resp.sendError(WebdavStatus.SC_NOT_FOUND, req.getRequestURI());
            } catch (WebdavException e) {
                resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
            } finally {
                resLocks.unlock(path, lockOwner);
            }
        } else {
            resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
        }

    }

}
