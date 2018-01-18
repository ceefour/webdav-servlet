package net.sf.webdav.util;

import org.apache.commons.lang3.StringUtils;

/**
 * 
 * This utility class can be used for maintaining string-based relative url's
 * 
 * @author Eric van der Steen
 */
public class URLUtil {

	/**
	 * Removes trailing / at the end of the path string, if present
	 * Adds leading / at the beginning of the oath string, if not present 
	 * 
	 * @param path
	 *            the path
	 * @return the path with leading / and without trailing /
	 */
	public static String getCleanPath(String path) {
		if (StringUtils.isNotEmpty(path)) {
			if(path.endsWith(CharsetUtil.FORWARD_SLASH)) {
				path = path.substring(0, path.length() - 1);
			}
			if(!path.startsWith(CharsetUtil.FORWARD_SLASH)) {
				path = CharsetUtil.FORWARD_SLASH + path;
			}
		}
		return path;
	}

	/**
	 * Creates a 'clean' path from a parentPath and a subPath. Subpath may be a folder or a filename.
	 * 
	 * @param parentPath
	 *            the parentPath
	 *  @param subPath
	 *            the subPath
	 * @return the path with leading / and without trailing /
	 */
	public static String getCleanPath(String parentPath, String subPath) {
		String path = getCleanPath(parentPath);
		if(subPath!=null) {
			// If path is root, then get rid of extra /
			if(CharsetUtil.FORWARD_SLASH.equals(path)) {
				path = "";
			}
			if(subPath.startsWith(CharsetUtil.FORWARD_SLASH)) {
				path += subPath;
			} else {
				path += CharsetUtil.FORWARD_SLASH + subPath;
			}
		}
		return path;
	}
	
	/**
	 * Creates a 'clean' parent path from the given path by removing the last '/' and
	 * everything after that.
	 * 
	 * @param path
	 *            the path
	 * @return parent path
	 */
	public static String getParentPath(String path) {
		// add leading and remove remove trailing forward-slash
		path = getCleanPath(path);
		// parent of root or empty path is null
		if(StringUtils.isEmpty(path) || CharsetUtil.FORWARD_SLASH.equals(path)) {
			return null;
		}
		// get the last forward-slash
		int slash = path.lastIndexOf(CharsetUtil.CHAR_FORWARD_SLASH);
		// if no forward-slash, parent is root
		return (slash != -1 ? path.substring(0, slash) : CharsetUtil.FORWARD_SLASH);
	}

	/**
	 * Return the 'clean' relative path for path, e.g. starting with /
	 * 
	 * @param request
	 *            The servlet request we are processing
	 */
	public static String getRelativePath(String path) {
		// add leading and remove remove trailing forward-slash
		path = getCleanPath(path);
		return (StringUtils.isNotEmpty(path) ? path : CharsetUtil.FORWARD_SLASH);
	}

}
