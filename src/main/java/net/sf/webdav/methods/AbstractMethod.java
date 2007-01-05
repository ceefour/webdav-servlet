package net.sf.webdav.methods;

import javax.servlet.http.HttpServletRequest;

public abstract class AbstractMethod {
    
    /**
     * size of the io-buffer
     */
    protected static int BUF_SIZE = 50000;
    /**
     * Return the relative path associated with this servlet.
     * 
     * @param request
     *            The servlet request we are processing
     */
    public static String getRelativePath(HttpServletRequest request) {

        // Are we being processed by a RequestDispatcher.include()?
        if (request.getAttribute("javax.servlet.include.request_uri") != null) {
            String result = (String) request
                    .getAttribute("javax.servlet.include.path_info");
            if (result == null)
                result = (String) request
                        .getAttribute("javax.servlet.include.servlet_path");
            if ((result == null) || (result.equals("")))
                result = "/";
            return (result);
        }

        // No, extract the desired path directly from the request
        String result = request.getPathInfo();
        if (result == null) {
            result = request.getServletPath();
        }
        if ((result == null) || (result.equals(""))) {
            result = "/";
        }
        return (result);

    }
}
