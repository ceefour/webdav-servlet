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
package nl.ellipsis.webdav.server.methods;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;

import org.springframework.http.HttpStatus;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import nl.ellipsis.webdav.server.IMimeTyper;
import nl.ellipsis.webdav.server.ITransaction;
import nl.ellipsis.webdav.server.IWebDAVStore;
import nl.ellipsis.webdav.server.StoredObject;
import nl.ellipsis.webdav.server.WebDAVConstants;
import nl.ellipsis.webdav.server.exceptions.AccessDeniedException;
import nl.ellipsis.webdav.server.exceptions.LockFailedException;
import nl.ellipsis.webdav.server.exceptions.WebDAVException;
import nl.ellipsis.webdav.server.locking.LockedObject;
import nl.ellipsis.webdav.server.locking.ResourceLocks;
import nl.ellipsis.webdav.server.util.CharsetUtil;
import nl.ellipsis.webdav.server.util.URLEncoder;
import nl.ellipsis.webdav.server.util.URLUtil;
import nl.ellipsis.webdav.server.util.XMLHelper;
import nl.ellipsis.webdav.server.util.XMLWriter;

public class DoPropfind extends AbstractMethod {

	private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DoPropfind.class);

	/**
	 * Array containing the safe characters set.
	 */
	protected static URLEncoder URL_ENCODER;

	/**
	 * PROPFIND - Specify a property mask.
	 */
	private static final int FIND_BY_PROPERTY = 0;

	/**
	 * PROPFIND - Display all properties.
	 */
	private static final int FIND_ALL_PROP = 1;

	/**
	 * PROPFIND - Return property names.
	 */
	private static final int FIND_PROPERTY_NAMES = 2;

	private IWebDAVStore _store;
	private ResourceLocks _resourceLocks;
	private IMimeTyper _mimeTyper;

	private int _depth;

	public DoPropfind(IWebDAVStore store, ResourceLocks resLocks, IMimeTyper mimeTyper) {
		_store = store;
		_resourceLocks = resLocks;
		_mimeTyper = mimeTyper;
	}

	public void execute(ITransaction transaction, HttpServletRequest req, HttpServletResponse resp)
			throws IOException, LockFailedException {
		String path = getRelativePath(req);
		if(LOG.isDebugEnabled()) {
			LOG.debug("-- " + this.getClass().getName()+" "+path);
		}

		// Retrieve the resources
		String tempLockOwner = "doPropfind" + System.currentTimeMillis() + req.toString();
		_depth = getDepth(req);

		if (_resourceLocks.lock(transaction, path, tempLockOwner, false, _depth, TEMP_TIMEOUT, TEMPORARY)) {

			StoredObject so = null;
			try {
				so = _store.getStoredObject(transaction, path);
				if (so == null) {
					resp.setContentType("text/xml; charset=UTF-8");
					resp.sendError(HttpServletResponse.SC_NOT_FOUND, req.getRequestURI());
					return;
				}

				Vector<String> properties = null;
				path = getRelativePath(req);

				int propertyFindType = FIND_ALL_PROP;
				Node propNode = null;

				if (req.getContentLength() != 0) {
					try {
						Document document = getDocument(req);
						// Get the root element of the document
						Element rootElement = document.getDocumentElement();

						propNode = XMLHelper.findSubElement(rootElement, "prop");
						if (propNode != null) {
							propertyFindType = FIND_BY_PROPERTY;
						} else if (XMLHelper.findSubElement(rootElement, "propname") != null) {
							propertyFindType = FIND_PROPERTY_NAMES;
						} else if (XMLHelper.findSubElement(rootElement, "allprop") != null) {
							propertyFindType = FIND_ALL_PROP;
						}
					} catch (Exception e) {
						resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
						return;
					}
				} else {
					// no content, which means it is a allprop request
					propertyFindType = FIND_ALL_PROP;
				}

				if (propertyFindType == FIND_BY_PROPERTY) {
					propertyFindType = 0;
					properties = XMLHelper.getPropertiesFromXML(propNode);
				}

				resp.setStatus(HttpStatus.MULTI_STATUS.value());
				resp.setContentType("text/xml; charset=UTF-8");

				// Create multistatus object
				XMLWriter generatedXML = new XMLWriter(resp.getWriter());
				generatedXML.writeXMLHeader();
				generatedXML.writeElement(NS_DAV_PREFIX,NS_DAV_FULLNAME,WebDAVConstants.XMLTag.MULTISTATUS,XMLWriter.OPENING);
				if (_depth == 0) {
					parseProperties(transaction, req, generatedXML, path, propertyFindType, properties);
				} else {
					recursiveParseProperties(transaction, path, req, generatedXML, propertyFindType, properties, _depth);
				}
				generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.MULTISTATUS,XMLWriter.CLOSING);

				generatedXML.sendData("doPropfind.response "+path+"\n");
			} catch (AccessDeniedException e) {
				resp.sendError(HttpServletResponse.SC_FORBIDDEN);
			} catch (WebDAVException e) {
				LOG.warn("Sending internal error!");
				resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			} finally {
				_resourceLocks.unlockTemporaryLockedObjects(transaction, path, tempLockOwner);
			}
		} else {
			Hashtable<String, Integer> errorList = new Hashtable<String, Integer>();
			errorList.put(path, HttpStatus.LOCKED.value());
			sendReport(req, resp, errorList);
		}
	}

	/**
	 * goes recursive through all folders. used by propfind
	 * 
	 * @param currentPath
	 *            the current path
	 * @param req
	 *            HttpServletRequest
	 * @param generatedXML
	 * @param propertyFindType
	 * @param properties
	 * @param depth
	 *            depth of the propfind
	 * @throws IOException
	 *             if an error in the underlying store occurs
	 */
	private void recursiveParseProperties(ITransaction transaction, String currentPath, HttpServletRequest req,
			XMLWriter generatedXML, int propertyFindType, Vector<String> properties, int depth)
			throws WebDAVException {

		parseProperties(transaction, req, generatedXML, currentPath, propertyFindType, properties);

		if (depth != 0) {
			// no need to get name if depth is already zero
			String[] names = _store.getChildrenNames(transaction, currentPath);
			names = names == null ? new String[] {} : names;
			for (String name : names) {
				recursiveParseProperties(transaction, URLUtil.getCleanPath(currentPath, name), req, generatedXML, propertyFindType, properties,
						depth - 1);
			}
		}
	}

	/**
	 * Propfind helper method.
	 * 
	 * @param req
	 *            The servlet request
	 * @param generatedXML
	 *            XML response to the Propfind request
	 * @param path
	 *            Path of the current resource
	 * @param type
	 *            Propfind type
	 * @param propertiesVector
	 *            If the propfind type is find properties by name, then this Vector
	 *            contains those properties
	 */
	private void parseProperties(ITransaction transaction, HttpServletRequest req, XMLWriter generatedXML, String path,
			int type, Vector<String> propertiesVector) throws WebDAVException {

		StoredObject so = _store.getStoredObject(transaction, path);

		boolean isFolder = so.isFolder();
		final String creationdate = creationDateFormat(so.getCreationDate());
		final String lastModified = lastModifiedDateFormat(so.getLastModified());
		String resourceLength = String.valueOf(so.getResourceLength());

		// ResourceInfo resourceInfo = new ResourceInfo(path, resources);

		generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.RESPONSE,XMLWriter.OPENING);
		String status = new String(
				"HTTP/1.1 " + HttpServletResponse.SC_OK + " " + HttpStatus.OK.getReasonPhrase());

		// Generating href element
		generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.HREF,XMLWriter.OPENING);

		String href = URLUtil.getCleanPath(req.getContextPath(),req.getServletPath());
		href = URLUtil.getCleanPath(href,path);
		// folders must end with slash
		if ((isFolder) && (!href.endsWith(CharsetUtil.FORWARD_SLASH))) {
			href += CharsetUtil.FORWARD_SLASH;
		}

		generatedXML.writeText(rewriteUrl(href));

		generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.HREF,XMLWriter.CLOSING);

		String resourceName = path;
		int lastSlash = path.lastIndexOf(CharsetUtil.CHAR_FORWARD_SLASH);
		if (lastSlash != -1)
			resourceName = resourceName.substring(lastSlash + 1);

		switch (type) {

		case FIND_ALL_PROP:

			generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.PROPSTAT, XMLWriter.OPENING);
			generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.PROP, XMLWriter.OPENING);

			generatedXML.writeProperty(NS_DAV_PREFIX,WebDAVConstants.XMLTag.CREATIONDATE, creationdate);
			generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.DISPLAYNAME, XMLWriter.OPENING);
			generatedXML.writeData(resourceName);
			generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.DISPLAYNAME, XMLWriter.CLOSING);
			if (!isFolder) {
				generatedXML.writeProperty(NS_DAV_PREFIX,WebDAVConstants.XMLTag.GET_LASTMODIFIED, lastModified);
				generatedXML.writeProperty(NS_DAV_PREFIX,WebDAVConstants.XMLTag.GET_CONTENTLENGTH, resourceLength);
				String contentType = (so.getMimeType()!=null ? so.getMimeType() : _mimeTyper.getMimeType(transaction, path));
				if (contentType != null) {
					generatedXML.writeProperty(NS_DAV_PREFIX,WebDAVConstants.XMLTag.GET_CONTENTTYPE, contentType);
				}
				generatedXML.writeProperty(NS_DAV_PREFIX,WebDAVConstants.XMLTag.GET_ETAG, getETag(so));
				generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.RESOURCETYPE, XMLWriter.NO_CONTENT);
			} else {
				generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.RESOURCETYPE, XMLWriter.OPENING);
				generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.COLLECTION, XMLWriter.NO_CONTENT);
				generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.RESOURCETYPE, XMLWriter.CLOSING);
			}

			writeSupportedLockElements(transaction, generatedXML, path);

			writeLockDiscoveryElements(transaction, generatedXML, path);

			generatedXML.writeProperty(NS_DAV_PREFIX,WebDAVConstants.XMLTag.SOURCE, "");
			generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.PROP, XMLWriter.CLOSING);
			generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.STATUS, XMLWriter.OPENING);
			generatedXML.writeText(status);
			generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.STATUS, XMLWriter.CLOSING);
			generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.PROPSTAT, XMLWriter.CLOSING);

			break;

		case FIND_PROPERTY_NAMES:

			generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.PROPSTAT, XMLWriter.OPENING);
			generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.PROP, XMLWriter.OPENING);

			generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.CREATIONDATE, XMLWriter.NO_CONTENT);
			generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.DISPLAYNAME, XMLWriter.NO_CONTENT);
			if (!isFolder) {
				generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.GET_CONTENTLANGUAGE, XMLWriter.NO_CONTENT);
				generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.GET_CONTENTLENGTH, XMLWriter.NO_CONTENT);
				generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.GET_CONTENTLENGTH, XMLWriter.NO_CONTENT);
				generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.GET_ETAG, XMLWriter.NO_CONTENT);
				generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.GET_LASTMODIFIED, XMLWriter.NO_CONTENT);
			}
			generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.RESOURCETYPE, XMLWriter.NO_CONTENT);
			generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.SUPPORTEDLOCK, XMLWriter.NO_CONTENT);
			generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.SOURCE, XMLWriter.NO_CONTENT);

			generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.PROP, XMLWriter.CLOSING);
			generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.STATUS, XMLWriter.OPENING);
			generatedXML.writeText(status);
			generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.STATUS, XMLWriter.CLOSING);
			generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.PROPSTAT, XMLWriter.CLOSING);

			break;

		case FIND_BY_PROPERTY:

			Vector<String> propertiesNotFound = new Vector<String>();

			// Parse the list of properties

			generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.PROPSTAT, XMLWriter.OPENING);
			generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.PROP, XMLWriter.OPENING);

			Enumeration<String> properties = propertiesVector.elements();

			while (properties.hasMoreElements()) {

				String property = (String) properties.nextElement();

				if (property.equals(WebDAVConstants.XMLTag.CREATIONDATE)) {
					generatedXML.writeProperty(NS_DAV_PREFIX,WebDAVConstants.XMLTag.CREATIONDATE, creationdate);
				} else if (property.equals("DAV::displayname")) {
					generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.DISPLAYNAME, XMLWriter.OPENING);
					generatedXML.writeData(resourceName);
					generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.DISPLAYNAME, XMLWriter.CLOSING);
				} else if (property.equals(WebDAVConstants.XMLTag.GET_CONTENTLANGUAGE)) {
					if (isFolder) {
						propertiesNotFound.addElement(property);
					} else {
						generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.GET_CONTENTLANGUAGE, XMLWriter.NO_CONTENT);
					}
				} else if (property.equals(WebDAVConstants.XMLTag.GET_CONTENTLENGTH)) {
					if (isFolder) {
						propertiesNotFound.addElement(property);
					} else {
						generatedXML.writeProperty(NS_DAV_PREFIX,WebDAVConstants.XMLTag.GET_CONTENTLENGTH, resourceLength);
					}
				} else if (property.equals(WebDAVConstants.XMLTag.GET_CONTENTTYPE)) {
					if (isFolder) {
						propertiesNotFound.addElement(property);
					} else {
						String mimeType = (so.getMimeType()!=null ? so.getMimeType() : _mimeTyper.getMimeType(transaction, path));
						generatedXML.writeProperty(NS_DAV_PREFIX,WebDAVConstants.XMLTag.GET_CONTENTTYPE, mimeType);
					}
				} else if (property.equals(WebDAVConstants.XMLTag.GET_ETAG)) {
					if (isFolder || so.isNullResource()) {
						propertiesNotFound.addElement(property);
					} else {
						generatedXML.writeProperty(NS_DAV_PREFIX,WebDAVConstants.XMLTag.GET_ETAG, getETag(so));
					}
				} else if (property.equals(WebDAVConstants.XMLTag.GET_LASTMODIFIED)) {
					if (isFolder && lastModified==null) {
						propertiesNotFound.addElement(property);
					} else {
						generatedXML.writeProperty(NS_DAV_PREFIX,WebDAVConstants.XMLTag.GET_LASTMODIFIED, lastModified);
					}
				} else if (property.equals(WebDAVConstants.XMLTag.RESOURCETYPE)) {
					if (isFolder) {
						generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.RESOURCETYPE, XMLWriter.OPENING);
						generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.COLLECTION, XMLWriter.NO_CONTENT);
						generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.RESOURCETYPE, XMLWriter.CLOSING);
					} else {
						generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.RESOURCETYPE, XMLWriter.NO_CONTENT);
					}
				} else if (property.equals(WebDAVConstants.XMLTag.SOURCE)) {
					generatedXML.writeProperty(NS_DAV_PREFIX,WebDAVConstants.XMLTag.SOURCE, "");
				} else if (property.equals(WebDAVConstants.XMLTag.SUPPORTEDLOCK)) {
					writeSupportedLockElements(transaction, generatedXML, path);
				} else if (property.equals(WebDAVConstants.XMLTag.LOCKDISCOVERY)) {
					writeLockDiscoveryElements(transaction, generatedXML, path);
				} else {
					propertiesNotFound.addElement(property);
				}
			}

			generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.PROP, XMLWriter.CLOSING);
			generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.STATUS, XMLWriter.OPENING);
			generatedXML.writeText(status);
			generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.STATUS, XMLWriter.CLOSING);
			generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.PROPSTAT, XMLWriter.CLOSING);

			Enumeration<String> propertiesNotFoundList = propertiesNotFound.elements();

			if (propertiesNotFoundList.hasMoreElements()) {

				status = new String("HTTP/1.1 " + HttpServletResponse.SC_NOT_FOUND + " "
						+ HttpStatus.NOT_FOUND.getReasonPhrase());

				generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.PROPSTAT, XMLWriter.OPENING);
				generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.PROP, XMLWriter.OPENING);

				while (propertiesNotFoundList.hasMoreElements()) {
					generatedXML.writeElement(NS_DAV_PREFIX,(String) propertiesNotFoundList.nextElement(), XMLWriter.NO_CONTENT);
				}

				generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.PROP, XMLWriter.CLOSING);
				generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.STATUS, XMLWriter.OPENING);
				generatedXML.writeText(status);
				generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.STATUS, XMLWriter.CLOSING);
				generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.PROPSTAT, XMLWriter.CLOSING);
			}
			break;
		}

		generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.RESPONSE, XMLWriter.CLOSING);

		so = null;
	}

	private void writeSupportedLockElements(ITransaction transaction, XMLWriter generatedXML, String path) {

		LockedObject lo = _resourceLocks.getLockedObjectByPath(transaction, path);

		generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.SUPPORTEDLOCK, XMLWriter.OPENING);

		if (lo == null) {
			// both locks (shared/exclusive) can be granted
			generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.LOCKENTRY, XMLWriter.OPENING);

			generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.LOCKSCOPE, XMLWriter.OPENING);
			generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.EXCLUSIVE, XMLWriter.NO_CONTENT);
			generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.LOCKSCOPE, XMLWriter.CLOSING);

			generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.LOCKTYPE, XMLWriter.OPENING);
			generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.WRITE, XMLWriter.NO_CONTENT);
			generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.LOCKTYPE, XMLWriter.CLOSING);

			generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.LOCKENTRY, XMLWriter.CLOSING);

			generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.LOCKENTRY, XMLWriter.OPENING);

			generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.LOCKSCOPE, XMLWriter.OPENING);
			generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.SHARED, XMLWriter.NO_CONTENT);
			generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.LOCKSCOPE, XMLWriter.CLOSING);

			generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.LOCKTYPE, XMLWriter.OPENING);
			generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.WRITE, XMLWriter.NO_CONTENT);
			generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.LOCKTYPE, XMLWriter.CLOSING);

			generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.LOCKENTRY, XMLWriter.CLOSING);

		} else {
			// LockObject exists, checking lock state
			// if an exclusive lock exists, no further lock is possible
			if (lo.isShared()) {

				generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.LOCKENTRY, XMLWriter.OPENING);

				generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.LOCKSCOPE, XMLWriter.OPENING);
				generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.SHARED, XMLWriter.NO_CONTENT);
				generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.LOCKSCOPE, XMLWriter.CLOSING);

				generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.LOCKTYPE, XMLWriter.OPENING);
				generatedXML.writeElement(NS_DAV_PREFIX,lo.getType(), XMLWriter.NO_CONTENT);
				generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.LOCKTYPE, XMLWriter.CLOSING);

				generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.LOCKENTRY, XMLWriter.CLOSING);
			}
		}

		generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.SUPPORTEDLOCK, XMLWriter.CLOSING);

		lo = null;
	}

	private void writeLockDiscoveryElements(ITransaction transaction, XMLWriter generatedXML, String path) {

		LockedObject lo = _resourceLocks.getLockedObjectByPath(transaction, path);

		if (lo != null && !lo.hasExpired()) {

			generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.LOCKDISCOVERY, XMLWriter.OPENING);
			generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.ACTIVELOCK, XMLWriter.OPENING);

			generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.LOCKTYPE, XMLWriter.OPENING);
			generatedXML.writeProperty(NS_DAV_PREFIX,lo.getType());
			generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.LOCKTYPE, XMLWriter.CLOSING);

			generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.LOCKSCOPE, XMLWriter.OPENING);
			if (lo.isExclusive()) {
				generatedXML.writeProperty(NS_DAV_PREFIX,WebDAVConstants.XMLTag.EXCLUSIVE);
			} else {
				generatedXML.writeProperty(NS_DAV_PREFIX,WebDAVConstants.XMLTag.SHARED);
			}
			generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.LOCKSCOPE, XMLWriter.CLOSING);

			generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.DEPTH, XMLWriter.OPENING);
			if (_depth == DEPTH_INFINITY) {
				generatedXML.writeText(S_DEPTH_INFINITY);
			} else {
				generatedXML.writeText(String.valueOf(_depth));
			}
			generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.DEPTH, XMLWriter.CLOSING);

			String[] owners = lo.getOwner();
			if (owners != null) {
				for (int i = 0; i < owners.length; i++) {
					generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.OWNER, XMLWriter.OPENING);
					generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.HREF, XMLWriter.OPENING);
					generatedXML.writeText(owners[i]);
					generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.HREF, XMLWriter.CLOSING);
					generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.OWNER, XMLWriter.CLOSING);
				}
			} else {
				generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.OWNER, XMLWriter.NO_CONTENT);
			}

			int timeout = (int) (lo.getTimeoutMillis() / 1000);
			String timeoutStr = new Integer(timeout).toString();
			generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.TIMEOUT, XMLWriter.OPENING);
			generatedXML.writeText("Second-" + timeoutStr);
			generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.TIMEOUT, XMLWriter.CLOSING);

			String lockToken = lo.getID();

			generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.LOCKTOKEN, XMLWriter.OPENING);
			generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.HREF, XMLWriter.OPENING);
			generatedXML.writeText("opaquelocktoken:" + lockToken);
			generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.HREF, XMLWriter.CLOSING);
			generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.LOCKTOKEN, XMLWriter.CLOSING);

			generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.ACTIVELOCK, XMLWriter.CLOSING);
			generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.LOCKDISCOVERY, XMLWriter.CLOSING);

		} else {
			generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.LOCKDISCOVERY, XMLWriter.NO_CONTENT);
		}

		lo = null;
	}

}
