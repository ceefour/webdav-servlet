package net.sf.webdav;

import net.sf.webdav.exceptions.UnauthenticatedException;
import net.sf.webdav.exceptions.WebdavException;
import net.sf.webdav.fromcatalina.MD5Encoder;
import net.sf.webdav.methods.AbstractMethod;
import net.sf.webdav.methods.DoCopy;
import net.sf.webdav.methods.DoDelete;
import net.sf.webdav.methods.DoGet;
import net.sf.webdav.methods.DoMkcol;
import net.sf.webdav.methods.DoMove;
import net.sf.webdav.methods.DoOptions;
import net.sf.webdav.methods.DoPropfind;
import net.sf.webdav.methods.DoPut;
import net.sf.webdav.methods.DoHead;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;

public class WebDavServletBean extends HttpServlet {

    /**
     * MD5 message digest provider.
     */
    private MessageDigest md5Helper;

    /**
     * The MD5 helper object for this class.
     */
    protected static final MD5Encoder md5Encoder = new MD5Encoder();

    /**
     * indicates that the store is readonly ?
     */
    private static final boolean readOnly = false;

    private ResourceLocks resLocks;

    private WebdavStore store;

    private int debug = -1;

    private DoGet doGet;
    private DoHead doHead;
    private DoDelete doDelete;
    private DoMove doMove;
    private DoCopy doCopy;
    private DoMkcol doMkcol;
    private DoOptions doOptions;
    private DoPut doPut;
    private DoPropfind doPropfind;


    public WebDavServletBean() {
        this.resLocks = new ResourceLocks();
        try {
            md5Helper = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException();
        }
    }

    public void init(WebdavStore store, String dftIndexFile,
            String insteadOf404, int nocontentLenghHeaders,
            boolean lazyFolderCreationOnPut, int debug) throws ServletException {

        this.store = store;
        this.debug = debug;

        MimeTyper mimeTyper = new MimeTyper() {
            public String getMimeType(String path) {
                return getServletContext().getMimeType(path);
            }
        };

        doGet = new DoGet(store, dftIndexFile, insteadOf404, resLocks, mimeTyper, nocontentLenghHeaders, debug);
        doHead = new DoHead(store, dftIndexFile, insteadOf404, resLocks, mimeTyper, nocontentLenghHeaders, debug);
        doDelete = new DoDelete(store, resLocks, readOnly, debug);
        doCopy = new DoCopy(store, resLocks, doDelete, readOnly, debug);
        doMove = new DoMove(resLocks, doDelete, doCopy, readOnly, debug);
        doMkcol = new DoMkcol(store, resLocks, readOnly, debug);
        doOptions = new DoOptions(store, resLocks, debug);
        doPut = new DoPut(store, resLocks, readOnly, debug, lazyFolderCreationOnPut);
        doPropfind = new DoPropfind(store, resLocks, readOnly, mimeTyper, debug);
    }

    /**
     * Handles the special WebDAV methods.
     */
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String method = req.getMethod();

        if (debug == 1) {
            System.out.println("-----------");
            System.out.println("WebdavServlet\n request: method = " + method);
            System.out.println("time: " + System.currentTimeMillis());
            System.out.println("path: " + req.getRequestURI() );
            System.out.println("-----------");
            Enumeration e = req.getHeaderNames();
            while (e.hasMoreElements()) {
                String s = (String) e.nextElement();
                System.out.println("header: " + s + " " + req.getHeader(s));
            }
            e = req.getAttributeNames();
            while (e.hasMoreElements()) {
                String s = (String) e.nextElement();
                System.out.println("attribute: " + s + " "
                        + req.getAttribute(s));
            }
            e = req.getParameterNames();
            while (e.hasMoreElements()) {
                String s = (String) e.nextElement();
                System.out.println("parameter: " + s + " "
                        + req.getParameter(s));
            }
        }

        try {
            store.begin(req.getUserPrincipal());
            store.checkAuthentication();
            resp.setStatus(WebdavStatus.SC_OK);

            try {
                if (method.equals("PROPFIND")) {
                    doPropfind.execute(req, resp);
                } else if (method.equals("PROPPATCH")) {
                    doProppatch(req, resp);
                } else if (method.equals("MKCOL")) {
                    doMkcol.execute(req, resp);
                } else if (method.equals("COPY")) {
                    doCopy.execute(req, resp);
                } else if (method.equals("MOVE")) {
                    doMove.execute(req, resp);
                } else if (method.equals("PUT")) {
                    doPut(req, resp);
                } else if (method.equals("GET")) {
                    doGet.execute(req, resp);
                } else if (method.equals("OPTIONS")) {
                    doOptions(req, resp);
                } else if (method.equals("HEAD")) {
                    doHead(req, resp);
                } else if (method.equals("DELETE")) {
                    doDelete.execute(req, resp);
                } else {
                    super.service(req, resp);
                }

                store.commit();
            } catch (IOException e) {
                e.printStackTrace();
                resp.sendError(WebdavStatus.SC_INTERNAL_SERVER_ERROR);
                store.rollback();
                throw new ServletException(e);
            }

        } catch (UnauthenticatedException e) {
            resp.sendError(WebdavStatus.SC_FORBIDDEN);
        } catch (WebdavException e) {
            e.printStackTrace();
            throw new ServletException(e);
        }
    }


    /**
     * OPTIONS Method.</br>
     * 
     * 
     * @param req
     *            HttpServletRequest
     * @param resp
     *            HttpServletResponse
     * @throws IOException
     *             if an error in the underlying store occurs
     */
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        doOptions.execute(req, resp);
    }

    /**
     * PROPPATCH Method.
     * 
     * @param req
     *            HttpServletRequest
     * @param resp
     *            HttpServletResponse
     * @throws IOException
     *             if an error in the underlying store occurs
     */
    protected void doProppatch(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        if (debug == 1)
            System.err.println("-- doProppatch");

        if (readOnly) {
            resp.sendError(WebdavStatus.SC_FORBIDDEN);

        } else

            resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
        // TODO implement proppatch
    }

    /**
     * HEAD Method.
     * 
     * @param req
     *            HttpServletRequest
     * @param resp
     *            HttpServletResponse
     * @throws IOException
     *             if an error in the underlying store occurs
     */
    protected void doHead(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (debug == 1)
            System.err.println("-- doHead");
        doHead.execute(req, resp);
    }

    /**
     * Process a POST request for the specified resource.
     * 
     * @param req
     *            The servlet request we are processing
     * @param resp
     *            The servlet response we are creating
     * 
     * @exception WebdavException
     *                if an error in the underlying store occurs
     */
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        doPut.execute(req, resp);
    }



}
