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
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Locale;
import java.util.TimeZone;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import nl.ellipsis.webdav.server.IMethodExecutor;
import nl.ellipsis.webdav.server.ITransaction;
import nl.ellipsis.webdav.server.StoredObject;
import nl.ellipsis.webdav.server.WebDAVConstants;
import nl.ellipsis.webdav.server.WebDAVStatus;
import nl.ellipsis.webdav.server.exceptions.LockFailedException;
import nl.ellipsis.webdav.server.locking.IResourceLocks;
import nl.ellipsis.webdav.server.locking.LockedObject;
import nl.ellipsis.webdav.server.util.CharsetUtil;
import nl.ellipsis.webdav.server.util.URLEncoder;
import nl.ellipsis.webdav.server.util.URLUtil;
import nl.ellipsis.webdav.server.util.XMLHelper;
import nl.ellipsis.webdav.server.util.XMLWriter;

public abstract class AbstractMethod implements IMethodExecutor {
	
	private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AbstractMethod.class);
	
	private static final ThreadLocal<DateFormat> thLastmodifiedDateFormat = new ThreadLocal<DateFormat>();
	private static final ThreadLocal<DateFormat> thCreationDateFormat = new ThreadLocal<DateFormat>();
	private static final ThreadLocal<DateFormat> thLocalDateFormat = new ThreadLocal<DateFormat>();

	/**
	 * Array containing the safe characters set.
	 */
	protected static URLEncoder URL_ENCODER;


	/**
	 * Simple date format for the creation date ISO 8601 representation (partial).
	 */
	protected static final String CREATION_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

	/**
	 * Simple date format for the last modified date. (RFC 822 updated by RFC 1123)
	 */
	protected static final String LAST_MODIFIED_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss z";

	protected static final String LOCAL_DATE_FORMAT = "dd/MM/yy' 'HH:mm:ss";
		
	protected static final int DEPTH_RESOURCE = 0; 
	protected static final int DEPTH_RESOURCE_WITH_CHLDREN = 1; 
	/**
	 * Default depth is infinite.
	 */	
	protected static final int DEPTH_INFINITY = -1; 
	protected static final String S_DEPTH_RESOURCE = "0"; 
	protected static final String S_DEPTH_RESOURCE_WITH_CHLDREN = "1"; 
	protected static final String S_DEPTH_INFINITY = "infinity"; 

	protected static final String PARAM_LOCKTOKEN = "locktoken";

	protected static final String NS_DAV_FULLNAME = "DAV:";
	protected static final String NS_DAV_PREFIX = "D";

	private static final String PROTOCOL_HTTP = "http";

	/**
	 * GMT timezone - all HTTP dates are on GMT
	 */
	protected static final String TIMEZONE_GMT = "GMT"; 

	static {
		URL_ENCODER = new URLEncoder();
		URL_ENCODER.addSafeCharacter(CharsetUtil.CHAR_DASH);
		URL_ENCODER.addSafeCharacter(CharsetUtil.CHAR_UNDERSCORE);
		URL_ENCODER.addSafeCharacter(CharsetUtil.CHAR_DOT);
		URL_ENCODER.addSafeCharacter(CharsetUtil.CHAR_ASTERIX);
		URL_ENCODER.addSafeCharacter(CharsetUtil.CHAR_FORWARD_SLASH);
	}

	/**
	 * size of the io-buffer
	 */
	protected static int BUF_SIZE = 65536;

	/**
	 * Default lock timeout value.
	 */
	protected static final int DEFAULT_TIMEOUT = 3600;

	/**
	 * Maximum lock timeout.
	 */
	protected static final int MAX_TIMEOUT = 604800;

	/**
	 * Boolean value to temporary lock resources (for method locks)
	 */
	protected static final boolean TEMPORARY = true;

	/**
	 * Timeout for temporary locks
	 */
	protected static final int TEMP_TIMEOUT = 10;
	
	public static String lastModifiedDateFormat(final Date date) {
		DateFormat df = thLastmodifiedDateFormat.get();
		if (df == null) {
			df = new SimpleDateFormat(LAST_MODIFIED_DATE_FORMAT, Locale.US);
			df.setTimeZone(TimeZone.getTimeZone(TIMEZONE_GMT));
			thLastmodifiedDateFormat.set(df);
		}
		return df.format(date);
	}

	public static String creationDateFormat(final Date date) {
		DateFormat df = thCreationDateFormat.get();
		if (df == null) {
			df = new SimpleDateFormat(CREATION_DATE_FORMAT);
			df.setTimeZone(TimeZone.getTimeZone(TIMEZONE_GMT));
			thCreationDateFormat.set(df);
		}
		return df.format(date);
	}

	public static String getLocalDateFormat(final Date date, final Locale loc) {
		DateFormat df = thLocalDateFormat.get();
		if (df == null) {
			df = new SimpleDateFormat(LOCAL_DATE_FORMAT, loc);
		}
		return df.format(date);
	}

	/**
	 * Return the relative path associated with this servlet.
	 * 
	 * @param request
	 *            The servlet request we are processing
	 */
	protected static String getRelativePath(HttpServletRequest request) {
		String path = null;
		// Are we being processed by a RequestDispatcher.include()?
		if (request.getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_PATH_INFO) != null) {
			path = (String) request.getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_PATH_INFO);
//			if (StringUtils.isEmpty(path)) {
//				path = (String) request.getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_SERVLET_PATH);
//			}
		}
		if(StringUtils.isEmpty(path)) {
			// No, extract the desired path directly from the request
			path = request.getPathInfo();
//			if (StringUtils.isEmpty(path)) {
//				path = request.getServletPath();
//			}
		}
		return URLUtil.getRelativePath(path);
	}
	
	/**
	 * Return W3C document 
	 * @throws IOException 
	 * @throws SAXException 
	 * @throws ServletException 
	 * @throws ParserConfigurationException 
	 */
	protected static Document getDocument(HttpServletRequest request) throws ServletException, SAXException, IOException, ParserConfigurationException {
		DocumentBuilder documentBuilder = XMLHelper.getDocumentBuilder();
		if(LOG.isDebugEnabled()) {
			String xml = IOUtils.toString(request.getInputStream(),java.nio.charset.StandardCharsets.UTF_8.name());
			LOG.debug(xml);
			return documentBuilder.parse(IOUtils.toInputStream(xml,java.nio.charset.StandardCharsets.UTF_8.name()));
		} else {
			return documentBuilder.parse(new InputSource(request.getInputStream()));
		}
	}

	/**
	 * reads the depth header from the request and returns it as a int
	 * 
	 * @param req
	 * @return the depth from the depth header
	 */
	protected static int getDepth(HttpServletRequest req) {
		int depth = DEPTH_INFINITY;
		String depthStr = req.getHeader(WebDAVConstants.HttpHeader.DEPTH);
		if (depthStr != null) {
			if (depthStr.equals(S_DEPTH_RESOURCE)) {
				depth = DEPTH_RESOURCE;
			} else if (depthStr.equals(S_DEPTH_RESOURCE_WITH_CHLDREN)) {
				depth = DEPTH_RESOURCE_WITH_CHLDREN;
			}
		}
		return depth;
	}

	/**
	 * URL rewriter.
	 * 
	 * @param path
	 *            Path which has to be rewiten
	 * @return the rewritten path
	 */
	protected static String rewriteUrl(String path) {
		return URL_ENCODER.encode(path);
	}

	/**
	 * Get the ETag associated with a file.
	 * 
	 * @param StoredObject
	 *            StoredObject to get resourceLength, lastModified and a hashCode of
	 *            StoredObject
	 * @return the ETag
	 */
	protected static String getETag(StoredObject so) {
		String resourceLength = "";
		String lastModified = "";
		if (so != null && so.isResource()) {
			resourceLength = Long.toString(so.getResourceLength());
			lastModified = Long.toString(so.getLastModified().getTime());
		}
		return "W/"+CharsetUtil.DQUOTE + resourceLength + CharsetUtil.CHAR_DASH + lastModified + CharsetUtil.DQUOTE;
	}

	protected static String[] getLockIdFromIfHeader(HttpServletRequest req) {
		String[] ids = new String[2];
		String id = req.getHeader(WebDAVConstants.HttpHeader.IF);

		if (StringUtils.isNotEmpty(id)) {
			// only one locktoken between parenthesis
			if (id.indexOf(CharsetUtil.GREATER_THAN+CharsetUtil.RIGHT_PARENTHESIS) == id.lastIndexOf(CharsetUtil.GREATER_THAN+CharsetUtil.RIGHT_PARENTHESIS)) {
				id = id.substring(id.indexOf(CharsetUtil.LEFT_PARENTHESIS+CharsetUtil.LESS_THAN), id.indexOf(CharsetUtil.GREATER_THAN+CharsetUtil.RIGHT_PARENTHESIS));

				if (id.indexOf(PARAM_LOCKTOKEN+CharsetUtil.COLON) != -1) {
					id = id.substring(id.indexOf(CharsetUtil.CHAR_COLON) + 1);
				}
				ids[0] = id;
			} else {
				String firstId = id.substring(id.indexOf(CharsetUtil.LEFT_PARENTHESIS+CharsetUtil.LESS_THAN), id.indexOf(CharsetUtil.GREATER_THAN+CharsetUtil.RIGHT_PARENTHESIS));
				if (firstId.indexOf(PARAM_LOCKTOKEN+CharsetUtil.COLON) != -1) {
					firstId = firstId.substring(firstId.indexOf(CharsetUtil.CHAR_COLON) + 1);
				}
				ids[0] = firstId;

				String secondId = id.substring(id.lastIndexOf(CharsetUtil.LEFT_PARENTHESIS+CharsetUtil.LESS_THAN), id.lastIndexOf(CharsetUtil.GREATER_THAN+CharsetUtil.RIGHT_PARENTHESIS));
				if (secondId.indexOf(PARAM_LOCKTOKEN+CharsetUtil.COLON) != -1) {
					secondId = secondId.substring(secondId.indexOf(CharsetUtil.CHAR_COLON) + 1);
				}
				ids[1] = secondId;
			}

		} else {
			ids = null;
		}
		return ids;
	}

	protected static String getLockIdFromLockTokenHeader(HttpServletRequest req) {
		String id = req.getHeader(WebDAVConstants.HttpHeader.LOCK_TOKEN);
		if (id != null) {
			id = id.substring(id.indexOf(CharsetUtil.CHAR_COLON) + 1, id.indexOf(CharsetUtil.CHAR_GREATER_THAN));

		}
		return id;
	}

	/**
	 * Checks if locks on resources at the given path exists and if so checks the
	 * If-Header to make sure the If-Header corresponds to the locked resource.
	 * Returning true if no lock exists or the If-Header is corresponding to the
	 * locked resource
	 * 
	 * @param req
	 *            Servlet request
	 * @param resp
	 *            Servlet response
	 * @param resourceLocks
	 * @param path
	 *            path to the resource
	 * @param errorList
	 *            List of error to be displayed
	 * @return true if no lock on a resource with the given path exists or if the
	 *         If-Header corresponds to the locked resource
	 * @throws IOException
	 * @throws LockFailedException
	 */
	protected static boolean checkLocks(ITransaction transaction, HttpServletRequest req, HttpServletResponse resp,
			IResourceLocks resourceLocks, String path) throws IOException, LockFailedException {

		LockedObject loByPath = resourceLocks.getLockedObjectByPath(transaction, path);
		if (loByPath != null) {

			if (loByPath.isShared()) {
				return true;
			}

			// the resource is locked
			String[] lockTokens = getLockIdFromIfHeader(req);
			String lockToken = null;
			if (lockTokens != null) {
				lockToken = lockTokens[0];
			} else {
				return false;
			}
			if (lockToken != null) {
				LockedObject loByIf = resourceLocks.getLockedObjectByID(transaction, lockToken);
				if (loByIf == null) {
					// no locked resource to the given lockToken
					return false;
				}
				if (!loByIf.equals(loByPath)) {
					loByIf = null;
					return false;
				}
				loByIf = null;
			}

		}
		loByPath = null;
		return true;
	}

	/**
	 * Send a multistatus element containing a complete error report to the client.
	 * If the errorList contains only one error, send the error directly without
	 * wrapping it in a multistatus message.
	 * 
	 * @param req
	 *            Servlet request
	 * @param resp
	 *            Servlet response
	 * @param errorList
	 *            List of error to be displayed
	 */
	protected static void sendReport(HttpServletRequest req, HttpServletResponse resp, Hashtable<String,Integer> errorList)
			throws IOException {

		if (errorList.size() == 1) {
			int code = errorList.elements().nextElement();
			String status = WebDAVStatus.getStatusText(code);
			if (status != null && !status.isEmpty()) {
				resp.sendError(code, status);
			} else {
				resp.sendError(code);
			}
		} else {
			resp.setStatus(WebDAVStatus.SC_MULTI_STATUS);

			String absoluteUri = req.getRequestURI();
			// String relativePath = getRelativePath(req);

			XMLWriter generatedXML = new XMLWriter();
			generatedXML.writeXMLHeader();

			generatedXML.writeElement(NS_DAV_PREFIX,NS_DAV_FULLNAME,WebDAVConstants.XMLTag.MULTISTATUS,XMLWriter.OPENING);

			Enumeration<String> pathList = errorList.keys();
			while (pathList.hasMoreElements()) {

				String errorPath = (String) pathList.nextElement();
				int errorCode = ((Integer) errorList.get(errorPath)).intValue();

				generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.RESPONSE,XMLWriter.OPENING);

				generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.HREF,XMLWriter.OPENING);
				String toAppend = null;
				if (absoluteUri.endsWith(errorPath)) {
					toAppend = absoluteUri;
				} else if (absoluteUri.contains(errorPath)) {
					int endIndex = absoluteUri.indexOf(errorPath) + errorPath.length();
					toAppend = absoluteUri.substring(0, endIndex);
				}
				if (StringUtils.isEmpty(toAppend)) {
					toAppend = CharsetUtil.FORWARD_SLASH;
				} else if (!toAppend.startsWith(CharsetUtil.FORWARD_SLASH) && !toAppend.startsWith(PROTOCOL_HTTP)) {
					toAppend = CharsetUtil.FORWARD_SLASH + toAppend;
				}
				generatedXML.writeText(errorPath);
				generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.HREF,XMLWriter.CLOSING);
				generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.STATUS,XMLWriter.OPENING);
				generatedXML.writeText("HTTP/1.1 " + errorCode + " " + WebDAVStatus.getStatusText(errorCode));
				generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.STATUS,XMLWriter.CLOSING);

				generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.RESPONSE,XMLWriter.CLOSING);

			}

			generatedXML.writeElement(NS_DAV_PREFIX,WebDAVConstants.XMLTag.MULTISTATUS,XMLWriter.CLOSING);

			Writer writer = resp.getWriter();
			writer.write(generatedXML.toString());
			writer.close();
		}
	}

}
