package net.sf.webdav;

/**
 * Listener for resource (un-)locking, enables handling of locking/unlocking of
 * resources for specific application needs
 * 
 * @author Dries Schulten
 */
public interface ILockingListener {

	/**
	 * Called when a lock is obtained for the specified resource
	 * 
	 * @param transaction
	 *            indicates that the method is within the scope of a WebDAV
	 * @param resourceUri
	 *            the resource denoted by this URI
	 */
	void onLockResource(ITransaction transaction, String resourceUri);

	/**
	 * Called when a lock is released for the specified resource
	 * 
	 * @param transaction
	 *            indicates that the method is within the scope of a WebDAV
	 * @param resourceUri
	 *            the resource denoted by this URI
	 */
	void onUnlockResource(ITransaction transaction, String resourceUri);
}
