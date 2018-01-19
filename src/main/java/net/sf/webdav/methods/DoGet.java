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
package net.sf.webdav.methods;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import net.sf.webdav.IMimeTyper;
import net.sf.webdav.ITransaction;
import net.sf.webdav.IWebDAVStore;
import net.sf.webdav.StoredObject;
import net.sf.webdav.WebDAVServlet;
import net.sf.webdav.WebDAVStatus;
import net.sf.webdav.locking.ResourceLocks;
import net.sf.webdav.util.URLUtil;

public class DoGet extends DoHead {

	private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DoGet.class);

	public DoGet(IWebDAVStore store, String dftIndexFile, String insteadOf404, ResourceLocks resourceLocks,
			IMimeTyper mimeTyper, int contentLengthHeader) {
		super(store, dftIndexFile, insteadOf404, resourceLocks, mimeTyper, contentLengthHeader);
	}

	protected void doBody(ITransaction transaction, HttpServletResponse resp, String path) {
		try {
			StoredObject so = _store.getStoredObject(transaction, path);
			if (so.isNullResource()) {
				String methodsAllowed = DeterminableMethod.determineMethodsAllowed(so);
				resp.addHeader("Allow", methodsAllowed);
				resp.sendError(WebDAVStatus.SC_METHOD_NOT_ALLOWED);
				return;
			}
			OutputStream out = resp.getOutputStream();
			InputStream in = _store.getResourceContent(transaction, path);
			try {
				int read = -1;
				byte[] copyBuffer = new byte[BUF_SIZE];

				while ((read = in.read(copyBuffer, 0, copyBuffer.length)) != -1) {
					out.write(copyBuffer, 0, read);
				}
			} finally {
				// flushing causes a IOE if a file is opened on the webserver
				// client disconnected before server finished sending response
				try {
					in.close();
				} catch (Exception e) {
					LOG.warn("Closing InputStream causes Exception!\n" + e.toString());
				}
				try {
					out.flush();
					out.close();
				} catch (Exception e) {
					LOG.warn("Flushing OutputStream causes Exception!\n" + e.toString());
				}
			}
		} catch (Exception e) {
			LOG.error(e.toString());
		}
	}

	protected void folderBody(ITransaction transaction, String path, HttpServletResponse resp, HttpServletRequest req)
			throws IOException {

		StoredObject so = _store.getStoredObject(transaction, path);
		if (so == null) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND, req.getRequestURI());
		} else {

			if (so.isNullResource()) {
				String methodsAllowed = DeterminableMethod.determineMethodsAllowed(so);
				resp.addHeader("Allow", methodsAllowed);
				resp.sendError(WebDAVStatus.SC_METHOD_NOT_ALLOWED);
				return;
			}

			if (so.isFolder()) {
				// TODO some folder response (for browsers, DAV tools
				// use propfind) in html?
				// Locale locale = req.getLocale();
				
				DateFormat shortDF = getDateTimeFormat(req.getLocale());
				resp.setContentType("text/html");
				resp.setCharacterEncoding("UTF8");
				String[] children = _store.getChildrenNames(transaction, path);
				// Make sure it's not null
				children = (children == null ? new String[] {} : children);
				// Sort by name
				Arrays.sort(children);
				
				String css = getCSS();
				
				if(WebDAVServlet.useVelocity) {
					Template t = Velocity.getTemplate("webdav.vm");
					VelocityContext context = new VelocityContext();
					context.put("path", path);
					context.put("css", css);
					Vector resources = new Vector();
					boolean isEven = false;
					for (String child : children) {
						isEven = !isEven;
						StoredObject obj = _store.getStoredObject(transaction, URLUtil.getCleanPath(path,child));
						resources.add(obj);
					}
					context.put("resources", resources);
					t.merge(context, resp.getWriter());				
				} else  {
					OutputStream out = resp.getOutputStream();
					
					StringBuilder sbFolderBody = new StringBuilder();
					sbFolderBody.append("<html><head><title>Content of folder ");
					sbFolderBody.append(path);
					sbFolderBody.append("</title><style type=\"text/css\">");
					sbFolderBody.append(css);
					sbFolderBody.append("</style></head>");
					sbFolderBody.append("<body>");
					sbFolderBody.append(getHeader(transaction, path, resp, req));
					sbFolderBody.append("<table>");
					sbFolderBody.append("<tr><th>Name</th><th>Size</th><th>Created</th><th>Modified</th></tr>");
					sbFolderBody.append("<tr>");
					sbFolderBody.append("<td colspan=\"4\"><a href=\"../\">Parent</a></td></tr>");
					boolean isEven = false;
					for (String child : children) {
						isEven = !isEven;
						StoredObject obj = _store.getStoredObject(transaction, URLUtil.getCleanPath(path,child));
						appendTableRow(transaction,sbFolderBody,path,child,obj,isEven,shortDF);
					}
					sbFolderBody.append("</table>");
					sbFolderBody.append(getFooter(transaction, path, resp, req));
					sbFolderBody.append("</body></html>");
					out.write(sbFolderBody.toString().getBytes("UTF-8"));
				}
			}
		}
	}

	/**
	 * Return the header to be displayed in front of the folder content
	 * 
	 * @param transaction
	 * @param path
	 * @param resp
	 * @param req
	 * @return
	 */
	private void appendTableRow(ITransaction transaction, StringBuilder sb, String resourcePath, String resourceName, StoredObject obj, boolean isEven, DateFormat df) {
		sb.append("<tr class=\"");
		sb.append(isEven ? "even" : "odd");
		sb.append("\">");
		sb.append("<td>");
		sb.append("<a href=\"");
		sb.append(resourceName);
		if (obj == null) {
			LOG.error("Should not return null for " + URLUtil.getCleanPath(resourcePath,resourceName));
		}
		if (obj != null && obj.isFolder()) {
			sb.append("/");
		}
		sb.append("\">");
		sb.append(resourceName);
		sb.append("</a></td>");
		if (obj != null && obj.isFolder()) {
			sb.append("<td>Folder</td>");
		} else {
			sb.append("<td>");
			if (obj != null) {
				sb.append(obj.getResourceLength());
			} else {
				sb.append("Unknown");
			}
			sb.append(" Bytes</td>");
		}
		if (obj != null && obj.getCreationDate() != null) {
			sb.append("<td>");
			sb.append(df.format(obj.getCreationDate()));
			sb.append("</td>");
		} else {
			sb.append("<td></td>");
		}
		if (obj != null && obj.getLastModified() != null) {
			sb.append("<td>");
			sb.append(df.format(obj.getLastModified()));
			sb.append("</td>");
		} else {
			sb.append("<td></td>");
		}
		sb.append("</tr>");
	}

	/**
	 * Return the CSS styles used to display the HTML representation of the webdav
	 * content.
	 * 
	 * @return
	 */
	private String getCSS() {
		// The default styles to use
		String retVal = "body {\n" + "	font-family: Arial, Helvetica, sans-serif;\n" + "}\n" + "h1 {\n"
				+ "	font-size: 1.5em;\n" + "}\n" + "th {\n" + "	background-color: #9DACBF;\n" + "}\n" + "table {\n"
				+ "	border-top-style: solid;\n" + "	border-right-style: solid;\n" + "	border-bottom-style: solid;\n"
				+ "	border-left-style: solid;\n" + "}\n" + "td {\n" + "	margin: 0px;\n" + "	padding-top: 2px;\n"
				+ "	padding-right: 5px;\n" + "	padding-bottom: 2px;\n" + "	padding-left: 5px;\n" + "}\n"
				+ "tr.even {\n" + "	background-color: #CCCCCC;\n" + "}\n" + "tr.odd {\n"
				+ "	background-color: #FFFFFF;\n" + "}\n" + "";
		try {
			// Try loading one via class loader and use that one instead
			ClassLoader cl = getClass().getClassLoader();
			InputStream iStream = cl.getResourceAsStream("webdav.css");
			if (iStream != null) {
				// Found css via class loader, use that one
				StringBuffer out = new StringBuffer();
				byte[] b = new byte[4096];
				for (int n; (n = iStream.read(b)) != -1;) {
					out.append(new String(b, 0, n));
				}
				retVal = out.toString();
			}
		} catch (Exception ex) {
			LOG.error("Error in reading webdav.css", ex);
		}

		return retVal;
	}

	/**
	 * Return the header to be displayed in front of the folder content
	 * 
	 * @param transaction
	 * @param path
	 * @param resp
	 * @param req
	 * @return
	 */
	private String getHeader(ITransaction transaction, String path, HttpServletResponse resp, HttpServletRequest req) {
		return "<h1>Content of folder " + path + "</h1>";
	}

	/**
	 * Return the footer to be displayed after the folder content
	 * 
	 * @param transaction
	 * @param path
	 * @param resp
	 * @param req
	 * @return
	 */
	private String getFooter(ITransaction transaction, String path, HttpServletResponse resp, HttpServletRequest req) {
		return "";
	}
	
	/**
	 * Return this as the Date/Time format for displaying Creation + Modification
	 * dates
	 *
	 * @param browserLocale
	 * @return DateFormat used to display creation and modification dates
	 */
	private DateFormat getDateTimeFormat(Locale browserLocale) {
		return SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.MEDIUM, browserLocale);
	}
}
