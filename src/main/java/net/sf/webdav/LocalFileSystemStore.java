/*
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
package net.sf.webdav;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.sf.webdav.exceptions.WebdavException;

/**
 * Reference Implementation of WebdavStore
 * 
 * @author joa
 * @author re
 */
public class LocalFileSystemStore implements WebdavStore {

    private static int BUF_SIZE = 50000;

    private File root = null;

    private int debug = -1;

    public LocalFileSystemStore(Integer debug, File root) {
        this.root = root;
        this.debug = debug.intValue();
    }

    public void begin(Principal principal) throws WebdavException {
        if (debug == 1)
            System.out.println("LocalFileSystemStore.begin()");
        if (!root.exists()) {
            if (!root.mkdirs()) {
                throw new WebdavException("root path: "
                        + root.getAbsolutePath()
                        + " does not exist and could not be created");
            }
        }
    }

    public void checkAuthentication() throws SecurityException {
        if (debug == 1)
            System.out.println("LocalFileSystemStore.checkAuthentication()");
        // do nothing

    }

    public void commit() throws WebdavException {
        // do nothing
        if (debug == 1)
            System.out.println("LocalFileSystemStore.commit()");
    }

    public void rollback() throws WebdavException {
        // do nothing
        if (debug == 1)
            System.out.println("LocalFileSystemStore.rollback()");

    }

    public boolean objectExists(String uri) throws WebdavException {
        File file = new File(root, uri);
        if (debug == 1)
            System.out.println("LocalFileSystemStore.objectExists(" + uri
                    + ")=" + file.exists());
        return file.exists();
    }

    public boolean isFolder(String uri) throws WebdavException {
        File file = new File(root, uri);
        if (debug == 1)
            System.out.println("LocalFileSystemStore.isFolder(" + uri + ")="
                    + file.isDirectory());
        return file.isDirectory();
    }

    public boolean isResource(String uri) throws WebdavException {
        File file = new File(root, uri);
        if (debug == 1)
            System.out.println("LocalFileSystemStore.isResource(" + uri + ") "
                    + file.isFile());
        return file.isFile();
    }

    public void createFolder(String uri) throws WebdavException {
        if (debug == 1)
            System.out
                    .println("LocalFileSystemStore.createFolder(" + uri + ")");
        File file = new File(root, uri);
        if (!file.mkdir())
            throw new WebdavException("cannot create folder: " + uri);
    }

    public void createResource(String uri) throws WebdavException {
        if (debug == 1)
            System.out.println("LocalFileSystemStore.createResource(" + uri
                    + ")");
        File file = new File(root, uri);
        try {
            if (!file.createNewFile())
                throw new WebdavException("cannot create file: " + uri);
        } catch (IOException e) {
            if (debug == 1)
                System.out.println("LocalFileSystemStore.createResource(" + uri
                        + ") failed");
            throw new WebdavException(e);
        }
    }

    /**
     * tries to save the given InputStream to the file at path "uri". content
     * type and charachter encoding are ignored
     */
    public void setResourceContent(String uri, InputStream is,
            String contentType, String characterEncoding)
            throws WebdavException {

        if (debug == 1)
            System.out.println("LocalFileSystemStore.setResourceContent(" + uri
                    + ")");
        File file = new File(root, uri);
        try {
            OutputStream os = new BufferedOutputStream(new FileOutputStream(
                    file));
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
            if (debug == 1)
                System.out.println("LocalFileSystemStore.setResourceContent("
                        + uri + ") failed");
            throw new WebdavException(e);
        }
    }

    public Date getLastModified(String uri) throws WebdavException {
        if (debug == 1)
            System.out.println("LocalFileSystemStore.getLastModified(" + uri
                    + ")");
        File file = new File(root, uri);
        return new Date(file.lastModified());
    }

    /**
     * @return the lastModified date of the file, java.io.file does not support
     *         a creation date
     */
    public Date getCreationDate(String uri) throws WebdavException {
        if (debug == 1)
            System.out.println("LocalFileSystemStore.getCreationDate(" + uri
                    + ")");
        // TODO return creation date instead of last modified
        File file = new File(root, uri);
        return new Date(file.lastModified());
    }

    /**
     * @return a (possibly empty) list of children, or <code>null</code> if
     *         the uri points to a file
     */
    public String[] getChildrenNames(String uri) throws WebdavException {
        if (debug == 1)
            System.out.println("LocalFileSystemStore.getChildrenNames(" + uri
                    + ")");
        File file = new File(root, uri);
        if (file.isDirectory()) {

            File[] children = file.listFiles();
            List childList = new ArrayList();
            for (int i = 0; i < children.length; i++) {
                String name = children[i].getName();
                childList.add(name);

            }
            String[] childrenNames = new String[childList.size()];
            childrenNames = (String[]) childList.toArray(childrenNames);
            return childrenNames;
        } else {
            return null;
        }

    }

    /**
     * @return an input stream to the specified resource
     */
    public InputStream getResourceContent(String uri) throws WebdavException {
        if (debug == 1)
            System.out.println("LocalFileSystemStore.getResourceContent(" + uri
                    + ")");
        File file = new File(root, uri);

        InputStream in;
        try {
            in = new BufferedInputStream(new FileInputStream(file));
        } catch (IOException e) {
            if (debug == 1)
                System.out.println("LocalFileSystemStore.getResourceContent("
                        + uri + ") failed");

            throw new WebdavException(e);
        }
        return in;
    }

    public long getResourceLength(String uri) throws WebdavException {
        if (debug == 1)
            System.out.println("LocalFileSystemStore.getResourceLength(" + uri
                    + ")");
        File file = new File(root, uri);
        return file.length();
    }

    public void removeObject(String uri) throws WebdavException {
        File file = new File(root, uri);
        boolean success = file.delete();
        if (debug == 1)
            System.out.println("LocalFileSystemStore.removeObject(" + uri
                    + ")=" + success);
        if (!success) {
            throw new WebdavException("cannot delete object: " + uri);
        }

    }

}
