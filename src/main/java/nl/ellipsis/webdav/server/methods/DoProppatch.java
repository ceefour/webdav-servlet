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
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
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
import nl.ellipsis.webdav.server.util.URLUtil;
import nl.ellipsis.webdav.server.util.XMLHelper;
import nl.ellipsis.webdav.server.util.XMLWriter;

public class DoProppatch extends AbstractMethod {

	private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DoProppatch.class);

	private boolean _readOnly;
	private IWebDAVStore _store;
	private ResourceLocks _resourceLocks;

	public DoProppatch(IWebDAVStore store, ResourceLocks resLocks, boolean readOnly) {
		_readOnly = readOnly;
		_store = store;
		_resourceLocks = resLocks;
	}

	public void execute(ITransaction transaction, HttpServletRequest req, HttpServletResponse resp)
			throws IOException, LockFailedException {
		String path = getRelativePath(req);
		if(LOG.isDebugEnabled()) {
			LOG.debug("-- " + this.getClass().getName()+" "+path);
		}

		if (_readOnly) {
			resp.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

		String parentPath = URLUtil.getParentPath(path);

		Hashtable<String, Integer> errorList = new Hashtable<String, Integer>();

		if (!checkLocks(transaction, req, resp, _resourceLocks, parentPath)) {
			resp.setStatus(HttpStatus.LOCKED.value());
			return; // parent is locked
		}

		if (!checkLocks(transaction, req, resp, _resourceLocks, path)) {
			resp.setStatus(HttpStatus.LOCKED.value());
			return; // resource is locked
		}

		// TODO for now, PROPPATCH just sends a valid response, stating that
		// everything is fine, but doesn't do anything.

		// Retrieve the resources
		String tempLockOwner = "doProppatch" + System.currentTimeMillis() + req.toString();

		if (_resourceLocks.lock(transaction, path, tempLockOwner, false, 0, TEMP_TIMEOUT, TEMPORARY)) {
			StoredObject so = null;
			LockedObject lo = null;
			try {
				so = _store.getStoredObject(transaction, path);
				lo = _resourceLocks.getLockedObjectByPath(transaction, path);

				if (so == null) {
					resp.sendError(HttpServletResponse.SC_NOT_FOUND);
					return;
					// we do not to continue since there is no root
					// resource
				}

				if (so.isNullResource()) {
					String methodsAllowed = DeterminableMethod.determineMethodsAllowed(so);
					resp.addHeader(javax.ws.rs.core.HttpHeaders.ALLOW, methodsAllowed);
					resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
					return;
				}

				String[] lockTokens = getLockIdFromIfHeader(req);
				boolean lockTokenMatchesIfHeader = (lockTokens != null && lockTokens[0].equals(lo.getID()));
				if (lo != null && lo.isExclusive() && !lockTokenMatchesIfHeader) {
					// Object on specified path is LOCKED
					errorList = new Hashtable<String, Integer>();
					errorList.put(path, new Integer(HttpStatus.LOCKED.value()));
					sendReport(req, resp, errorList);
					return;
				}

				List<String> toset = null;
				List<String> toremove = null;
				List<String> tochange = new Vector<String>();
				// contains all properties from
				// toset and toremove

				path = getRelativePath(req);

				Node tosetNode = null;
				Node toremoveNode = null;

				if (req.getContentLength() != 0) {
					try {
						Document document = getDocument(req);
						// Get the root element of the document
						Element rootElement = document.getDocumentElement();

						tosetNode = XMLHelper.findSubElement(XMLHelper.findSubElement(rootElement, "set"), "prop");
						toremoveNode = XMLHelper.findSubElement(XMLHelper.findSubElement(rootElement, "remove"),
								"prop");
					} catch (Exception e) {
						LOG.error("Sending internal error!", e);
						resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
						return;
					}
				} else {
					// no content: error
					LOG.error("Sending internal error - No content was provided (should that actually be a bad request?)");
					resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					return;
				}

				if (tosetNode != null) {
					toset = XMLHelper.getPropertiesFromXML(tosetNode);
					tochange.addAll(toset);
				}

				if (toremoveNode != null) {
					toremove = XMLHelper.getPropertiesFromXML(toremoveNode);
					tochange.addAll(toremove);
				}

				resp.setStatus(HttpStatus.MULTI_STATUS.value());
				resp.setContentType("text/xml; charset=UTF-8");

				// Create multistatus object
				XMLWriter generatedXML = new XMLWriter(resp.getWriter());
				generatedXML.writeXMLHeader();
				generatedXML.writeElement(NS_DAV_PREFIX,NS_DAV_FULLNAME,WebDAVConstants.XMLTag.MULTISTATUS, XMLWriter.OPENING);

				generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.RESPONSE, XMLWriter.OPENING);
				String status = new String("HTTP/1.1 " + HttpServletResponse.SC_OK + " " + HttpStatus.OK.getReasonPhrase());

				// Generating href element
				generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.HREF, XMLWriter.OPENING);

				String href = URLUtil.getCleanPath(req.getContextPath(),path);
				// folders must end with slash
				if ((so.isFolder()) && (!href.endsWith(CharsetUtil.FORWARD_SLASH))) {
					href += CharsetUtil.FORWARD_SLASH;
				}

				generatedXML.writeText(rewriteUrl(href));

				generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.HREF, XMLWriter.CLOSING);

				for (Iterator<String> iter = tochange.iterator(); iter.hasNext();) {
					String property = (String) iter.next();

					generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.PROPSTAT, XMLWriter.OPENING);

					generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.PROP, XMLWriter.OPENING);
					generatedXML.writeElement(NS_DAV_PREFIX,property, XMLWriter.NO_CONTENT);
					generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.PROP, XMLWriter.CLOSING);

					generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.STATUS, XMLWriter.OPENING);
					generatedXML.writeText(status);
					generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.STATUS, XMLWriter.CLOSING);

					generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.PROPSTAT, XMLWriter.CLOSING);
				}

				generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.RESPONSE, XMLWriter.CLOSING);

				generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.MULTISTATUS, XMLWriter.CLOSING);

				generatedXML.sendData("doPropPatch "+path+"/n");
			} catch (AccessDeniedException e) {
				resp.sendError(HttpServletResponse.SC_FORBIDDEN);
			} catch (WebDAVException e) {
				LOG.error("Sending internal error!", e);
				resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			} finally {
				_resourceLocks.unlockTemporaryLockedObjects(transaction, path, tempLockOwner);
			}
		} else {
			LOG.error("Sending internal error - Failed to lock");
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}
}
