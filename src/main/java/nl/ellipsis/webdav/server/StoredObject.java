/*
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package nl.ellipsis.webdav.server;

import java.util.Date;

import org.apache.commons.lang.StringUtils;

import nl.ellipsis.webdav.server.util.CharsetUtil;

public class StoredObject {
	
	private final String url;
	private final String name;

    private boolean isFolder;
    private Date lastModified;
    private Date creationDate;
    private long contentLength;
    private String  mimeType;

    private boolean isNullRessource;
    
    public StoredObject(String relativeUrl) {
    	this.url = relativeUrl;
    	this.name = getResourceName(relativeUrl);
    }

    /**
	 * Gets the date of the creation
	 * 
	 * @return creation Date
	 */
	public Date getCreationDate() {
	    return (creationDate);
	}

	/**
	 * Gets the date of the last modification
	 * 
	 * @return last modification Date
	 */
	public Date getLastModified() {
	    return (lastModified);
	}

	/**
	 * Retrieve the myme type from the store object.
	 * Can also return NULL if the store does not handle
	 * mime type stuff.
	 * In that case the mime type is determined by the servletcontext
	 *
	 * @return the mimeType
	 */
	public String getMimeType() {
	    return mimeType;
	}
	
	/**
	 * Gets the name of the resource 
	 * 
	 * @return name of the resource
	 */
	public String getName() {
	    return name;
	}


	/**
	 * Gets the length of the resource content
	 * 
	 * @return length of the resource content
	 */
	public long getResourceLength() {
	    return (contentLength);
	}

	/**
	 * Gets the relative uri of the resource 
	 * 
	 * @return relative uri of the resource
	 */
	public String getUrl() {
		return url;
	}

	/**
     * Determines whether the StoredObject is a folder or a resource
     * 
     * @return true if the StoredObject is a collection
     */
    public boolean isFolder() {
        return (isFolder);
    }

    /**
	 * Gets the state of the resource
	 * 
	 * @return true if the resource is in lock-null state
	 */
	public boolean isNullResource() {
	    return isNullRessource;
	}

	/**
     * Determines whether the StoredObject is a folder or a resource
     * 
     * @return true if the StoredObject is a resource
     */
    public boolean isResource() {
        return (!isFolder);
    }

    /**
	 * Sets the date of the creation
	 * 
	 * @param d
	 *      date of the creation
	 */
	public void setCreationDate(Date c) {
	    this.creationDate = c;
	}

	/**
     * Sets a new StoredObject as a collection or resource
     * 
     * @param f
     *      true - collection ; false - resource
     */
    public void setFolder(boolean f) {
        this.isFolder = f;
    }

    /**
     * Sets the date of the last modification
     * 
     * @param d
     *      date of the last modification
     */
    public void setLastModified(Date d) {
        this.lastModified = d;
    }

    /**
	 * Set the mime type of this object
	 * 
	 * @param mimeType the mimeType to set
	 */
	public void setMimeType(String mimeType) {
	    this.mimeType = mimeType;
	}

	/**
	 * Sets a StoredObject as a lock-null resource
	 * 
	 * @param f
	 *      true to set the resource as lock-null resource
	 */
	public void setNullResource(boolean f) {
	    this.isNullRessource = f;
	    this.isFolder = false;
	    this.creationDate = null;
	    this.lastModified = null;
	    // this.content = null;
	    this.contentLength = 0;
	    this.mimeType= null;
	}

	/**
     * Sets the length of the resource content
     * 
     * @param l
     *      the length of the resource content
     */
    public void setResourceLength(long l) {
        this.contentLength = l;
    }

	private static String getResourceName(String relativeUrl) {
		String retval = null;
		if(StringUtils.isNotEmpty(relativeUrl)) {
			String[] s = relativeUrl.split(CharsetUtil.FORWARD_SLASH);
			if(s.length>0) {
				retval = s[s.length-1];
			}
		}
		return retval;
	}

}
