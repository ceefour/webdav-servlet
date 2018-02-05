/*
 * Copyright 1999,2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nl.ellipsis.webdav.server;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import javax.servlet.ServletException;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.app.Velocity;

import nl.ellipsis.webdav.server.exceptions.WebDAVException;
import nl.ellipsis.webdav.server.util.CharsetUtil;

/**
 * Servlet which provides support for WebDAV level 2.
 * 
 * the original class is org.apache.catalina.servlets.WebdavServlet by Remy
 * Maucherat, which was heavily changed
 * 
 * @author Remy Maucherat
 */

public class WebDAVServlet extends WebDAVServletBean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7124585997065835079L;
	
	private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(WebDAVServlet.class);

	private static final String INIT_PARAM_CREATE_ROOT_IF_NOT_EXISTS = "createRootIfNotExists";
	private static final String INIT_PARAM_DEFAULT_INDEX_FILE = "default-index-file";
	private static final String INIT_PARAM_INSTEAD_OF_404 = "instead-of-404";
	private static final String INIT_PARAM_LAZY_FOLDER_CREATION_ON_PUT = "lazyFolderCreationOnPut";
	private static final String INIT_PARAM_NO_CONTENT_LENGTH_HEADERS = "no-content-length-headers";
	private static final String INIT_PARAM_RESOURCE_HANDLER_IMPL = "ResourceHandlerImplementation";
	private static final String INIT_PARAM_ROOTPATH = "rootpath";
	private static final String INIT_PARAM_ROOTPATH_WAR_FILE_ROOT_VALUE = "*WAR-FILE-ROOT*";
	
	public static boolean useVelocity = false;

	public void init() throws ServletException {

		// Parameters from web.xml
		String clazzName = getServletConfig().getInitParameter(INIT_PARAM_RESOURCE_HANDLER_IMPL);
		if (StringUtils.isEmpty(clazzName)) {
			clazzName = LocalFileSystemStore.class.getName();
		}

		boolean createRootIfNotExists = getBooleanInitParameter(INIT_PARAM_CREATE_ROOT_IF_NOT_EXISTS, false);
		File root = getFileRoot(createRootIfNotExists);

		IWebDAVStore webdavStore = constructStore(clazzName, root);

		boolean lazyFolderCreationOnPut = getBooleanInitParameter(INIT_PARAM_LAZY_FOLDER_CREATION_ON_PUT, false);
		String dftIndexFile = getInitParameter(INIT_PARAM_DEFAULT_INDEX_FILE);
		String insteadOf404 = getInitParameter(INIT_PARAM_INSTEAD_OF_404);
		int noContentLengthHeader = getIntInitParameter(INIT_PARAM_NO_CONTENT_LENGTH_HEADERS, -1);
		
		/**
		 *  Use singletron pattern to create and initialize the Velocity engine
		 */
		if(useVelocity) {
			Velocity.setProperty("resource.loader", "webapp");
			Velocity.setProperty("webapp.resource.loader.class", "org.apache.velocity.tools.view.WebappResourceLoader");
			Velocity.setProperty("webapp.resource.loader.path", "/WEB-INF/velocity/");
			Velocity.setApplicationAttribute("javax.servlet.ServletContext", getServletConfig().getServletContext());
			Velocity.init();	
		}

		super.init(webdavStore, dftIndexFile, insteadOf404, noContentLengthHeader, lazyFolderCreationOnPut);
	}

	protected IWebDAVStore constructStore(String clazzName, File root) {
		IWebDAVStore webdavStore;
		try {
			Class<?> clazz = WebDAVServlet.class.getClassLoader().loadClass(clazzName);

			Constructor<?> ctor = clazz.getConstructor(new Class[] { File.class });

			webdavStore = (IWebDAVStore) ctor.newInstance(new Object[] { root });
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("some problem making store component", e);
		}
		return webdavStore;
	}

	private boolean getBooleanInitParameter(String key, boolean defaultValue) {
		String value = getInitParameter(key);
		return value == null ? defaultValue : ("1".equals(value) || Boolean.getBoolean(value));
	}

	private int getIntInitParameter(String key, int defaultValue) {
		String value = getInitParameter(key);
		return value == null ? defaultValue : Integer.parseInt(value);
	}

	private File getFileRoot(boolean createRootIfNotExists) {
		File root = null;
		String rootPath = getInitParameter(INIT_PARAM_ROOTPATH);
		if (StringUtils.isEmpty(rootPath)) {
			throw new WebDAVException("missing parameter: " + INIT_PARAM_ROOTPATH);
		}
		if (rootPath.equals(INIT_PARAM_ROOTPATH_WAR_FILE_ROOT_VALUE)) {
			String file = LocalFileSystemStore.class.getProtectionDomain().getCodeSource().getLocation().getFile().replace(CharsetUtil.CHAR_BACKSLASH, CharsetUtil.CHAR_FORWARD_SLASH);
			if (file.charAt(0) == CharsetUtil.CHAR_FORWARD_SLASH && System.getProperty("os.name").indexOf("Windows") != -1) {
				file = file.substring(1, file.length());
			}

			int ix = file.indexOf("/WEB-INF/");
			if (ix != -1) {
				rootPath = file.substring(0, ix).replace(CharsetUtil.CHAR_FORWARD_SLASH, File.separatorChar);
			} else {
				throw new WebDAVException("Could not determine root of war file. Can't extract from path '" + file + "' for this web container");
			}
		}
		LOG.info("Mountpoint set to "+rootPath);
		root = new File(rootPath);
		boolean createdRoot = false;
		if(!root.exists() && createRootIfNotExists) {
			createdRoot = root.mkdir();
		}
		if(!root.exists()) {
			if(createRootIfNotExists && !createdRoot) {
				LOG.error("Unable to create mountpoint "+rootPath+"!");
			} else {
				LOG.error("Mountpoint "+rootPath+" does not exist!");
			}
			root = null;
		} else {
			LOG.info("Mountpoint set to "+ (createdRoot ? "newly created " : "") + rootPath);
		}
		return root;
	}

}
