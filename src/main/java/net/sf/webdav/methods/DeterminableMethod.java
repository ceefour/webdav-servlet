package net.sf.webdav.methods;

public class DeterminableMethod extends AbstractMethod {

    /**
     * Determines the methods normally allowed for the resource.
     *
     * @param exists
     *            does the resource exist?
     * @param isFolder
     *            is the resource a folder?
     * @return all allowed methods, separated by commas
     */
    protected String determineMethodsAllowed(boolean exists, boolean isFolder) {
        StringBuffer methodsAllowed = new StringBuffer();
        try {
            if (exists) {
                methodsAllowed
                        .append("OPTIONS, GET, HEAD, POST, DELETE, TRACE");
                methodsAllowed
                        .append(", PROPPATCH, COPY, MOVE, LOCK, UNLOCK, PROPFIND");
                if (isFolder) {
                    methodsAllowed.append(", PUT");
                }
                return methodsAllowed.toString();
            }
        } catch (Exception e) {
            // we do nothing, just return less allowed methods

        }
        methodsAllowed.append("OPTIONS, MKCOL, PUT, LOCK");
        return methodsAllowed.toString();

    }


}
