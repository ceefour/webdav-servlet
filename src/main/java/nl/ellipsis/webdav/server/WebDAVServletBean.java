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

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.ellipsis.webdav.server.exceptions.UnauthenticatedException;
import nl.ellipsis.webdav.server.exceptions.WebDAVException;
import nl.ellipsis.webdav.server.locking.ResourceLocks;
import nl.ellipsis.webdav.server.methods.DoCopy;
import nl.ellipsis.webdav.server.methods.DoDelete;
import nl.ellipsis.webdav.server.methods.DoGet;
import nl.ellipsis.webdav.server.methods.DoHead;
import nl.ellipsis.webdav.server.methods.DoLock;
import nl.ellipsis.webdav.server.methods.DoMkcol;
import nl.ellipsis.webdav.server.methods.DoMove;
import nl.ellipsis.webdav.server.methods.DoNotImplemented;
import nl.ellipsis.webdav.server.methods.DoOptions;
import nl.ellipsis.webdav.server.methods.DoPropfind;
import nl.ellipsis.webdav.server.methods.DoProppatch;
import nl.ellipsis.webdav.server.methods.DoPut;
import nl.ellipsis.webdav.server.methods.DoUnlock;
import nl.ellipsis.webdav.server.util.MD5Encoder;

public class WebDAVServletBean extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7635231240070637329L;

	private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(WebDAVServletBean.class);

	/**
	 * MD5 message digest provider.
	 */
	protected static MessageDigest MD5_HELPER;

	/**
	 * The MD5 helper object for this class.
	 */
	protected static final MD5Encoder MD5_ENCODER = new MD5Encoder();

	private static final boolean READ_ONLY = false;
	protected ResourceLocks _resLocks;
	protected IWebDAVStore _store;
	private HashMap<String, IMethodExecutor> _methodMap = new HashMap<String, IMethodExecutor>();

	public WebDAVServletBean() {
		_resLocks = new ResourceLocks();

		try {
			MD5_HELPER = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException();
		}
	}

	public void init(IWebDAVStore store, String dftIndexFile, String insteadOf404, int nocontentLenghHeaders,
			boolean lazyFolderCreationOnPut) throws ServletException {

		_store = store;

		IMimeTyper mimeTyper = new IMimeTyper() {
			public String getMimeType(ITransaction transaction, String path) {
				String retVal = _store.getStoredObject(transaction, path).getMimeType();
				if (retVal == null) {
					retVal = getServletContext().getMimeType(path);
				}
				return retVal;
			}
		};

		register("GET", new DoGet(store, dftIndexFile, insteadOf404, _resLocks, mimeTyper, nocontentLenghHeaders));
		register("HEAD", new DoHead(store, dftIndexFile, insteadOf404, _resLocks, mimeTyper, nocontentLenghHeaders));
		DoDelete doDelete = (DoDelete) register("DELETE", new DoDelete(store, _resLocks, READ_ONLY));
		DoCopy doCopy = (DoCopy) register("COPY", new DoCopy(store, _resLocks, doDelete, READ_ONLY));
		register("LOCK", new DoLock(store, _resLocks, READ_ONLY));
		register("UNLOCK", new DoUnlock(store, _resLocks, READ_ONLY));
		register("MOVE", new DoMove(_resLocks, doDelete, doCopy, READ_ONLY));
		register("MKCOL", new DoMkcol(store, _resLocks, READ_ONLY));
		register("OPTIONS", new DoOptions(store, _resLocks));
		register("PUT", new DoPut(store, _resLocks, READ_ONLY, lazyFolderCreationOnPut));
		register("PROPFIND", new DoPropfind(store, _resLocks, mimeTyper));
		register("PROPPATCH", new DoProppatch(store, _resLocks, READ_ONLY));
		register("*NO*IMPL*", new DoNotImplemented(READ_ONLY));
	}

	@Override
	public void destroy() {
		if (_store != null) {
			_store.destroy();
		}
		super.destroy();
	}

	protected IMethodExecutor register(String methodName, IMethodExecutor method) {
		_methodMap.put(methodName, method);
		return method;
	}

	/**
	 * Handles the special WebDAV methods.
	 */
	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		String methodName = req.getMethod();
		ITransaction transaction = null;
		boolean needRollback = false;

		if (LOG.isDebugEnabled()) {
			debugRequest(methodName, req);
		}

		try {
			Principal userPrincipal = getUserPrincipal(req);
			transaction = _store.begin(userPrincipal);
			needRollback = true;
			_store.checkAuthentication(transaction);
			resp.setStatus(WebDAVStatus.SC_OK);

			try {
				IMethodExecutor methodExecutor = (IMethodExecutor) _methodMap.get(methodName);
				if (methodExecutor == null) {
					methodExecutor = (IMethodExecutor) _methodMap.get("*NO*IMPL*");
				}

				methodExecutor.execute(transaction, req, resp);

				_store.commit(transaction);
				/**
				 * Clear not consumed data
				 *
				 * Clear input stream if available otherwise later access include current input.
				 * These cases occur if the client sends a request with body to a nonexisting resource.
				 */
				if (req.getContentLength() != 0 && req.getInputStream().available() > 0) {
					if (LOG.isDebugEnabled()) {
						LOG.debug("Clear not consumed data!");
					}
					while (req.getInputStream().available() > 0) {
						req.getInputStream().read();
					}
				}
				needRollback = false;
			} catch (IOException e) {
				java.io.StringWriter sw = new java.io.StringWriter();
				java.io.PrintWriter pw = new java.io.PrintWriter(sw);
				e.printStackTrace(pw);
				LOG.error("IOException: " + sw.toString());
				resp.sendError(WebDAVStatus.SC_INTERNAL_SERVER_ERROR);
				_store.rollback(transaction);
				throw new ServletException(e);
			}

		} catch (UnauthenticatedException e) {
			resp.sendError(WebDAVStatus.SC_FORBIDDEN);
		} catch (WebDAVException e) {
			java.io.StringWriter sw = new java.io.StringWriter();
			java.io.PrintWriter pw = new java.io.PrintWriter(sw);
			e.printStackTrace(pw);
			LOG.error("WebDAVException: " + sw.toString());
			throw new ServletException(e);
		} catch (Exception e) {
			java.io.StringWriter sw = new java.io.StringWriter();
			java.io.PrintWriter pw = new java.io.PrintWriter(sw);
			e.printStackTrace(pw);
			LOG.error("Exception: " + sw.toString());
		} finally {
			if (needRollback) {
				_store.rollback(transaction);
			}
		}

	}

	/**
	 * Method that permit to customize the way user information are extracted from
	 * the request, default use JAAS
	 * 
	 * @param req
	 * @return
	 */
	protected Principal getUserPrincipal(HttpServletRequest req) {
		return req.getUserPrincipal();
	}

	private void debugRequest(String methodName, HttpServletRequest req) {
		LOG.debug("-----------");
		LOG.debug("Request: methodName = " + methodName);
		LOG.debug("time: " + System.currentTimeMillis());
		LOG.debug("path: " + req.getRequestURI());
		LOG.debug("-----------");
		Enumeration<?> e = req.getHeaderNames();
		while (e.hasMoreElements()) {
			String s = (String) e.nextElement();
			LOG.debug("header: " + s + " " + req.getHeader(s));
		}
		e = req.getAttributeNames();
		while (e.hasMoreElements()) {
			String s = (String) e.nextElement();
			LOG.debug("attribute: " + s + " " + req.getAttribute(s));
		}
		e = req.getParameterNames();
		while (e.hasMoreElements()) {
			String s = (String) e.nextElement();
			LOG.debug("parameter: " + s + " " + req.getParameter(s));
		}
	}

}
