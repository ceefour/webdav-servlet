package net.sf.webdav.exceptions;

public class LockFailedException extends WebDAVException {

    /**
	 * 
	 */
	private static final long serialVersionUID = 3424181138150125232L;

	public LockFailedException() {
        super();
    }

    public LockFailedException(String message) {
        super(message);
    }

    public LockFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public LockFailedException(Throwable cause) {
        super(cause);
    }
}
