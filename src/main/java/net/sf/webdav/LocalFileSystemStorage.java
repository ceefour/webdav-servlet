/**
 * 
 */
package net.sf.webdav;

import java.io.InputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.BufferedInputStream;
import java.io.FileInputStream;

import java.security.Principal;

import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.ArrayList;

/**
 * Reference Implementation of IWebdavStorage
 * 
 * @author joa
 * 
 */
public class LocalFileSystemStorage implements IWebdavStorage {

	private static final String ROOTPATH_PARAMETER = "rootpath";

	private static final String DEBUG_PARAMETER = "storeDebug";

	private static int BUF_SIZE = 50000;

	private static File root = null;

	private static int debug = -1;

	public void begin(Principal principal, Hashtable parameters)
			throws Exception {
		if (debug == -1) {
			String debugString = (String) parameters.get(DEBUG_PARAMETER);
			if (debugString == null) {
				debug = 0;
			}else{
			debug = Integer.parseInt(debugString);
			}
		}
		if (debug == 1)
			System.out.println("LocalFileSystemStore.begin()");
		if (LocalFileSystemStorage.root == null) {

			String rootPath = (String) parameters.get(ROOTPATH_PARAMETER);
			if (rootPath == null) {
				throw new Exception("missing parameter: " + ROOTPATH_PARAMETER);
			}
			LocalFileSystemStorage.root = new File(rootPath);
			if (!LocalFileSystemStorage.root.exists()) {
				if (!LocalFileSystemStorage.root.mkdirs()) {
					throw new Exception(ROOTPATH_PARAMETER + ": "
							+ LocalFileSystemStorage.root
							+ " does not exist and could not be created");
				}
			}
		}
	}

	public void checkAuthentication() throws SecurityException {
		if (debug == 1)
			System.out.println("LocalFileSystemStore.checkAuthentication()");
		// do nothing

	}

	public void commit() throws IOException {
		// do nothing
		if (debug == 1)
			System.out.println("LocalFileSystemStore.commit()");
	}

	public void rollback() throws IOException {
		// do nothing
		if (debug == 1)
			System.out.println("LocalFileSystemStore.rollback()");

	}

	public boolean objectExists(String uri) throws IOException {
		File file = new File(root, uri);
		if (debug == 1)
			System.out.println("LocalFileSystemStore.objectExists(" + uri
					+ ")=" + file.exists());
		return file.exists();
	}

	public boolean isFolder(String uri) throws IOException {
		File file = new File(root, uri);
		if (debug == 1)
			System.out.println("LocalFileSystemStore.isFolder(" + uri + ")="
					+ file.isDirectory());
		return file.isDirectory();
	}

	public boolean isResource(String uri) throws IOException {
		File file = new File(root, uri);
		if (debug == 1)
			System.out.println("LocalFileSystemStore.isResource(" + uri + ") "
					+ file.isFile());
		return file.isFile();
	}

	/**
	 * @throws IOException
	 *             if the folder cannot be created
	 * @throws SecurityException
	 *             if the creation of the folder is not permitted
	 */
	public void createFolder(String uri) throws IOException {
		if (debug == 1)
			System.out
					.println("LocalFileSystemStore.createFolder(" + uri + ")");
		File file = new File(root, uri);
		if (!file.mkdir())
			throw new IOException("cannot create folder: " + uri);
	}

	/**
	 * @throws IOException
	 *             if the resource cannot be created
	 */
	public void createResource(String uri) throws IOException {
		if (debug == 1)
			System.out.println("LocalFileSystemStore.createResource(" + uri
					+ ")");
		File file = new File(root, uri);
		if (!file.createNewFile())
			throw new IOException("cannot create file: " + uri);
	}

	/**
	 * tries to save the given InputStream to the file at path "uri". content
	 * type and charachter encoding are ignored
	 */
	public void setResourceContent(String uri, InputStream is,
			String contentType, String characterEncoding) throws IOException {

		if (debug == 1)
			System.out.println("LocalFileSystemStore.setResourceContent(" + uri
					+ ")");
		File file = new File(root, uri);
		OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
		try {
			int read = -1;
			byte[] copyBuffer = new byte[BUF_SIZE];

			while ((read = is.read(copyBuffer, 0, copyBuffer.length)) != -1) {
				os.write(copyBuffer, 0, read);
			}
		} finally {
			try {
				is.close();
			} finally {
				os.close();
			}
		}
	}

	/**
	 * @return the lastModified Date
	 */
	public Date getLastModified(String uri) throws IOException {
		if (debug == 1)
			System.out.println("LocalFileSystemStore.getLastModified(" + uri
					+ ")");
		File file = new File(root, uri);
		return new Date(file.lastModified());
	}

	/**
	 * @return the lastModified date of the file, java.io.file does not support
	 *         a creation date
	 */
	public Date getCreationDate(String uri) throws IOException {
		if (debug == 1)
			System.out.println("LocalFileSystemStore.getCreationDate(" + uri
					+ ")");
		// TODO return creation date instead of last modified
		File file = new File(root, uri);
		return new Date(file.lastModified());
	}

	/**
	 * @return a (possibly empty) list of children, or <code>null</code> if
	 *         the uri points to a file
	 */
	public String[] getChildrenNames(String uri) throws IOException {
		if (debug == 1)
			System.out.println("LocalFileSystemStore.getChildrenNames(" + uri
					+ ")");
		File file = new File(root, uri);
		if (file.isDirectory()) {

			File[] children = file.listFiles();
			List childList = new ArrayList();
			for (int i = 0; i < children.length; i++) {
				String name = children[i].getName();
				childList.add(name);

			}
			String[] childrenNames = new String[childList.size()];
			childrenNames = (String[]) childList.toArray(childrenNames);
			return childrenNames;
		} else {
			return null;
		}

	}

	/**
	 * @return an input stream to the specified resource
	 */
	public InputStream getResourceContent(String uri) throws IOException {
		if (debug == 1)
			System.out.println("LocalFileSystemStore.getResourceContent(" + uri
					+ ")");
		File file = new File(root, uri);

		InputStream in = new BufferedInputStream(new FileInputStream(file));
		return in;
	}

	/**
	 * @return the size of the file
	 */
	public long getResourceLength(String uri) throws IOException {
		if (debug == 1)
			System.out.println("LocalFileSystemStore.getResourceLength(" + uri
					+ ")");
		File file = new File(root, uri);
		return file.length();
	}

	/**
	 * @throws IOException
	 *             if the deletion failed
	 * 
	 */
	public void removeObject(String uri) throws IOException {
		File file = new File(root, uri);
		boolean success = file.delete();
		if (debug == 1)
			System.out.println("LocalFileSystemStore.removeObject(" + uri
					+ ")=" + success);
		if (!success) {
			throw new IOException("cannot delete object: " + uri);
		}

	}

}
