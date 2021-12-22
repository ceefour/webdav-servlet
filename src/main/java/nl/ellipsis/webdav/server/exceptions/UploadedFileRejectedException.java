package nl.ellipsis.webdav.server.exceptions;

public class UploadedFileRejectedException extends WebDAVException {

    public UploadedFileRejectedException() {
        super();
    }

    public UploadedFileRejectedException(String message) {
        super(message);
    }

    public UploadedFileRejectedException(String message, Throwable cause) {
        super(message, cause);
    }

    public UploadedFileRejectedException(Throwable cause) {
        super(cause);
    }
}
