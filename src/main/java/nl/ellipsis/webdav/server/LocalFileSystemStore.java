/*
 * Copyright 2004 The Apache Software Foundation
 * Copyright 2018 Ellipsis BV, Netherlands
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
 *
 */
package nl.ellipsis.webdav.server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import nl.ellipsis.webdav.server.exceptions.WebDAVException;
import nl.ellipsis.webdav.server.util.URLUtil;

/**
 * Reference Implementation of WebdavStore
 * 
 * @author joa
 * @author re
 */
public class LocalFileSystemStore implements IWebDAVStore {

	private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(LocalFileSystemStore.class);

	private static int BUF_SIZE = 65536;

	private File _root = null;

	public LocalFileSystemStore(File root) {
		_root = root;
	}

	public void destroy() {
		LOG.debug("LocalFileSystemStore.destroy()");
	}

	public ITransaction begin(Principal principal) throws WebDAVException {
		LOG.debug("LocalFileSystemStore.begin()");
		if (!_root.exists()) {
			if (!_root.mkdirs()) {
				String msg = "root path '" + _root.getAbsolutePath() + "' does not exist and could not be created";
				LOG.error("LocalFileSystemStore.begin() failed: "+msg);
				throw new WebDAVException(msg);
			}
		}
		return null;
	}

	public void checkAuthentication(ITransaction transaction) throws SecurityException {
		LOG.debug("LocalFileSystemStore.checkAuthentication()");
		// do nothing
	}

	public void commit(ITransaction transaction) throws WebDAVException {
		// do nothing
		LOG.debug("LocalFileSystemStore.commit()");
	}

	public void rollback(ITransaction transaction) throws WebDAVException {
		// do nothing
		LOG.debug("LocalFileSystemStore.rollback()");
	}

	public void createFolder(ITransaction transaction, String uri) throws WebDAVException {
		LOG.debug("LocalFileSystemStore.createFolder(" + uri + ")");
		File file = new File(_root, uri);
		if (!file.mkdir()) {
			LOG.error("LocalFileSystemStore.createFolder(" + uri + ") failed");
			throw new WebDAVException("cannot create folder '" + uri+"'");
		}
	}

	public void createResource(ITransaction transaction, String uri) throws WebDAVException {
		LOG.debug("LocalFileSystemStore.createResource(" + uri + ")");
		File file = new File(_root, uri);
		try {
			if (!file.createNewFile()) {
				throw new WebDAVException("cannot create file '" + uri+"'");
			}
		} catch (IOException e) {
			LOG.error("LocalFileSystemStore.createResource(" + uri + ") failed");
			throw new WebDAVException("cannot create file '" + uri+"'",e);
		}
	}

	public long setResourceContent(ITransaction transaction, String uri, InputStream is, String contentType,
			String characterEncoding) throws WebDAVException {
		LOG.debug("LocalFileSystemStore.setResourceContent(" + uri + ")");
		File file = new File(_root, uri);
		try {
			OutputStream os = new BufferedOutputStream(new FileOutputStream(file), BUF_SIZE);
			try {
				int read;
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
		} catch (IOException e) {
			LOG.error("LocalFileSystemStore.setResourceContent(" + uri + ") failed");
			throw new WebDAVException(e);
		}
		return getResourceLength(file);
	}

	public String[] getChildrenNames(ITransaction transaction, String uri) throws WebDAVException {
		LOG.debug("LocalFileSystemStore.getChildrenNames(" + uri + ")");
		File file = new File(_root, uri);
		String[] childrenNames = null;
		if (file.isDirectory()) {
			File[] children = file.listFiles();
			if (children != null) {
				List<String> childList = new ArrayList<String>();
				String name = null;
				for (int i = 0; i < children.length; i++) {
					name = children[i].getName();
					childList.add(name);
					LOG.debug("\tChild " + i + ": " + name);
				}
				childrenNames = new String[childList.size()];
				childrenNames = (String[]) childList.toArray(childrenNames);
			}
		}
		return childrenNames;
	}

	public void removeObject(ITransaction transaction, String uri) throws WebDAVException {
		File file = new File(_root, uri);
		boolean success = file.delete();
		LOG.debug("LocalFileSystemStore.removeObject(" + uri + ")=" + success);
		if (!success) {
			throw new WebDAVException("cannot remove object '" + uri+"'");
		}
	}

	public InputStream getResourceContent(ITransaction transaction, String uri) throws WebDAVException {
		LOG.debug("LocalFileSystemStore.getResourceContent(" + uri + ")");
		File file = new File(_root, uri);

		InputStream in;
		try {
			in = new BufferedInputStream(new FileInputStream(file));
		} catch (IOException e) {
			LOG.error("LocalFileSystemStore.getResourceContent(" + uri + ") failed");
			throw new WebDAVException(e);
		}
		return in;
	}

	public long getResourceLength(ITransaction transaction, String uri) {
		LOG.debug("LocalFileSystemStore.getResourceLength(" + uri + ")");
		File file = new File(_root, uri);
		return getResourceLength(file);
	}

	public StoredObject getStoredObject(ITransaction transaction, String uri) {
		uri = URLUtil.getCleanPath(uri);
		if(uri.contains("//")) {
			LOG.debug("ERROR: LocalFileSystemStore.getStoredObject(" + uri + ")");
		}
		LOG.debug("LocalFileSystemStore.getStoredObject(" + uri + ")");
		StoredObject so = null;
		File file = new File(_root, uri);
		if (file.exists()) {
			try {
				so = new StoredObject(uri);
				so.setFolder(file.isDirectory());
				so.setResourceLength(getResourceLength(file));
				so.setLastModified(new Date(file.lastModified()));
				// set as many attributes as possible using nio
				BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
				so.setCreationDate(new Date(attr.creationTime().toMillis()));
				so.setMimeType(Files.probeContentType(Paths.get(file.toURI())));
			} catch (IOException e) {
				LOG.error("LocalFileSystemStore.getStoredObject(" + uri + ") failed",e);
			}
		}
		return so;
	}
	
	private long getResourceLength(File file) {
		long length = -1;
		try {
			length = file.length();
		} catch (SecurityException e) {
			LOG.error("LocalFileSystemStore.getResourceLength(" + file.getAbsolutePath() + ") failed" + "\nCannot get file.length");
		}
		return length;
	}

}
