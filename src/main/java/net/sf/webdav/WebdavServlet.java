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

package net.sf.webdav;

import java.io.File;
import java.lang.reflect.Constructor;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.servlet.ServletException;

import net.sf.webdav.exceptions.WebdavException;

/**
 * Servlet which provides support for WebDAV level 2.
 * 
 * the original class is org.apache.catalina.servlets.WebdavServlet by Remy
 * Maucherat, which was heavily changed
 * 
 * @author Remy Maucherat
 */

public class WebdavServlet extends WebDavServletBean {
    private static final String DEBUG_SERVLET_PARAMETER = "servletDebug";

    private static final String DEBUG_STORE_PARAMETER = "storeDebug";

    private static final String ROOTPATH_PARAMETER = "rootpath";

    public void init() throws ServletException {
        try {
            setMd5Helper(MessageDigest.getInstance("MD5"));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException();
        }

        // Parameters from web.xml
        String clazzName = getServletConfig().getInitParameter(
                "ResourceHandlerImplementation");

        try {
            Class clazz = WebdavServlet.class.getClassLoader().loadClass(
                    clazzName);
            // IWebdavStorage store = factory.getStore();
            String debugStoreString = (String) getInitParameter(DEBUG_STORE_PARAMETER);
            int storeDebug = -1;
            if (debugStoreString != null) {
                storeDebug = Integer.parseInt(debugStoreString);
            }
            Constructor ctor = clazz.getConstructor(new Class[] { Integer.class,
                    File.class });

            File root = getFileRoot();
            setStore((IWebdavStorage) ctor.newInstance(new Object[] {
                    new Integer(storeDebug), root }));
        } catch (Exception e) {
            throw new ServletException(e);
        }

        String debugString = (String) getInitParameter(DEBUG_SERVLET_PARAMETER);
        if (debugString == null) {
            setDebug(-1);
        } else {
            setDebug(Integer.parseInt(debugString));
        }

        boolean lazyFolderCreationOnPut = getInitParameter("lazyFolderCreationOnPut") != null
                && getInitParameter("lazyFolderCreationOnPut").equals("1");
        setLazyFolderCreationOnPut(lazyFolderCreationOnPut);
    }

    private File getFileRoot() {
        String rootPath = (String) getInitParameter(ROOTPATH_PARAMETER);
        if (rootPath == null) {
            throw new WebdavException("missing parameter: "
                    + ROOTPATH_PARAMETER);
        }
        if (rootPath.equals("*WAR-FILE-ROOT*")) {
            String file = LocalFileSystemStorage.class.getProtectionDomain()
                    .getCodeSource().getLocation().getFile().replace('\\', '/');
            if (file.charAt(0) == '/'
                    && System.getProperty("os.name").indexOf("Windows") != -1) {
                file = file.substring(1, file.length());
            }

            int ix = file.indexOf("/WEB-INF/");
            if (ix != -1) {
                rootPath = file.substring(0, ix).replace('/',
                        File.separatorChar);
            } else {
                throw new WebdavException(
                        "Could not determine root of war file. Can't extract from path '"
                                + file + "' for this web container");
            }
        }
        return new File(rootPath);
    }

}
