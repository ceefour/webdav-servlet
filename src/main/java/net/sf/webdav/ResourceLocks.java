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

import java.io.IOException;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Vector;

/**
 * some very simple locking management for concurrent data access, NOT the
 * webdav locking
 * 
 * @author re
 */
public class ResourceLocks {

	private Hashtable fLocks = null;

	private IWebdavStorage fStore = null;

	/**
	 * 
	 */
	protected ResourceLocks(IWebdavStorage store) {
		this.fStore = store;
		this.fLocks = new Hashtable();
	}

	/**
	 * locks the resource at "path" (and all subfolders if existing). Either all
	 * resources at "path" are locked, or none if locking of at least one
	 * subelement failed
	 * 
	 * 
	 * @param path
	 * @return true if the resource at path was successfully locked, false if it
	 *         was already locked before
	 * @throws IOException
	 *             if something goes wrong on store level
	 */
	protected synchronized boolean lock(String path) throws IOException {
		Vector children = new Vector();
		children.add(path);

		// generate temporary locks
		Hashtable tempLockStore = new Hashtable();
		tempLockStore = generateLocks(getParentPath(path), children);
		// check if no new locks are allready existing
		if (checkLocks(tempLockStore)) {
			fLocks.putAll(tempLockStore);
			return true;
		} else {
			return false;
		}

	}

	/**
	 * 
	 * @param locks
	 *            a hashtable with locks
	 * @return true if none of new locks is already in use
	 */
	private boolean checkLocks(Hashtable locks) {
		Enumeration tempKeys = locks.keys();
		while (tempKeys.hasMoreElements()) {
			String key = (String) tempKeys.nextElement();
			if (this.fLocks.containsKey(key)) {
				// a parent directory matches
				Vector children = (Vector) this.fLocks.get(key);
				Vector tempChildren = (Vector) locks.get(key);
				for (int i = 0; i < tempChildren.size(); i++) { // (int i = 0; i
																// <
																// tempChildren.size();
																// i++)
					if (this.fLocks.containsKey((String) tempChildren
							.elementAt(i))) {

						// one of the new children is already a key
						return false;
					}
					for (int i2 = 0; i2 < children.size(); i2++) {
						if (((String) children.elementAt(i2))
								.equals((String) tempChildren.elementAt(i))) {
							// one of the new children already is an old child
							return false;
						}
					}
				}
			}
		}
		return true;
	}

	/**
	 * generates a Hashtable with locks for a given parentPath and its children
	 * 
	 * @param parentPath
	 *            a String of the parent path
	 * @param childrenVector
	 *            a Vector with the children names (String) of parentPath
	 * @return a Hashtable with the generated Locks
	 * @throws IOException
	 */
	private Hashtable generateLocks(String parentPath, Vector childrenVector)
			throws IOException {

		Hashtable tempLockStore = new Hashtable();
		if (childrenVector.size() > 0) {
			tempLockStore.put(parentPath, childrenVector);

			// ******************LOGGING****************
			// System.out.println("to parentPath(key)\""+parentPath+"\" added
			// "+childrenVector.size()+" children:");
			// for(int i=0;i<childrenVector.size();i++){
			// System.out.println((String)childrenVector.elementAt(i));
			// }
			// *****************************************

		}
		// add all given children to the lock

		for (int childCount = 0; childCount < childrenVector.size(); childCount++) {
			String pathString = (String) childrenVector.elementAt(childCount);

			if (fStore.isFolder(pathString)) {
				String[] children = fStore.getChildrenNames(pathString);
				Vector newPath = new Vector(children.length);

				for (int i = 0; i < children.length; i++) {
					newPath.add(pathString + "/" + children[i]);
				}
				tempLockStore.putAll(generateLocks(pathString, newPath));

			} else {
				// a file or not existant -> probably a new resource locked
				// before creating
				// do nothing special
				// or some weird thing that is neither a file nor an object but
				// still exists
			}

		}
		return tempLockStore;
	}

	/**
	 * 
	 * @param path
	 * @return true if the resource at "path" is locked, false otherwise
	 */
	protected synchronized boolean isLocked(String path) {

		String parentPath = getParentPath(path);
		if (this.fLocks.containsKey(parentPath)) {
			Vector children = (Vector) this.fLocks.get(parentPath);
			return children.contains(path);
		} else {
			return false;
		}
	}

	/**
	 * unlocks the resource at "path" (and all subfolders if existing)<p/> does
	 * NOT check if the removal is allowed!
	 * 
	 * @param path
	 * @throws IOException
	 */
	protected synchronized void unlock(String path) throws IOException {

		Vector children = new Vector();
		children.add(path);

		Hashtable tempLockStore = generateLocks(getParentPath(path), children);
		Enumeration tempKeys = tempLockStore.keys();
		while (tempKeys.hasMoreElements()) {
			String key = (String) tempKeys.nextElement();
			if (this.fLocks.containsKey(key)) {
				// a parent directory matches
				children = (Vector) this.fLocks.get(key);
				Vector tempChildren = (Vector) tempLockStore.get(key);

				children.removeAll(tempChildren);

				if (children.isEmpty()) {
					this.fLocks.remove(key);
				}
			}
		}
	}

	private String getParentPath(String path) {
		int slash = path.lastIndexOf('/');
		if (slash != -1) {
			return path.substring(0, slash);
		}
		return null;
	}
}
