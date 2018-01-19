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
		if (path!=null) {
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
		subPath = getCleanPath(subPath);
		if(path==null) {
			path = subPath;
		} else if(subPath!=null) {
			// If path is root, then get rid of extra /
			if(CharsetUtil.FORWARD_SLASH.equals(path)) {
				path = "";
			}
			path += subPath;
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
		return (slash>0 ? path.substring(0, slash) : CharsetUtil.FORWARD_SLASH);
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
	
	/**
	 * Return a context-relative path, beginning with a /, that represents the
	 * canonical version of the specified path after ".." and "." elements are
	 * resolved out. If the specified path attempts to go outside the boundaries of
	 * the current context (i.e. too many ".." path elements are present), return
	 * <code>null</code> instead.
	 * 
	 * @param path
	 *            Path to be normalized
	 * @return normalized path
	 */
	public static String normalize(String path) {

		if (path == null)
			return null;

		// Create a place for the normalized path
		String normalized = path;

		if (normalized.equals(CharsetUtil.FORWARD_SLASH+CharsetUtil.DOT))
			return CharsetUtil.FORWARD_SLASH;

		// Normalize the slashes and add leading slash if necessary
		if (normalized.indexOf(CharsetUtil.CHAR_BACKSLASH) >= 0)
			normalized = normalized.replace(CharsetUtil.CHAR_BACKSLASH, CharsetUtil.CHAR_FORWARD_SLASH);
		if (!normalized.startsWith(CharsetUtil.FORWARD_SLASH))
			normalized = CharsetUtil.FORWARD_SLASH + normalized;

		// Resolve occurrences of "//" in the normalized path
		while (true) {
			int index = normalized.indexOf(CharsetUtil.FORWARD_SLASH+CharsetUtil.FORWARD_SLASH);
			if (index < 0)
				break;
			normalized = normalized.substring(0, index) + normalized.substring(index + 1);
		}

		// Resolve occurrences of "/./" in the normalized path
		while (true) {
			int index = normalized.indexOf(CharsetUtil.FORWARD_SLASH+CharsetUtil.DOT+CharsetUtil.FORWARD_SLASH);
			if (index < 0)
				break;
			normalized = normalized.substring(0, index) + normalized.substring(index + 2);
		}

		// Resolve occurrences of "/../" in the normalized path
		while (true) {
			int index = normalized.indexOf(CharsetUtil.FORWARD_SLASH+CharsetUtil.DOT+CharsetUtil.DOT+CharsetUtil.FORWARD_SLASH);
			if (index < 0)
				break;
			if (index == 0)
				return (null); // Trying to go outside our context
			int index2 = normalized.lastIndexOf(CharsetUtil.CHAR_FORWARD_SLASH, index - 1);
			normalized = normalized.substring(0, index2) + normalized.substring(index + 3);
		}

		// Return the normalized path that we have completed
		return normalized;
	}

}
