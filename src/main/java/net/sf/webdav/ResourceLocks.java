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

	// TODO implement some cleanup to remove locks that were left after a crash
	// or something alike

	/**
	 * keys: parent path value: vector with all children of parent path (as
	 * LockObject)
	 */
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
	 * subelement failed<p/> every method call must have a unique owner
	 * 
	 * 
	 * @param path
	 *            what resource to lock
	 * @param owner
	 *            the owner of the lock
	 * @param exclusive
	 *            if the lock should be exclusive (or shared)
	 * @return true if the resource at path was successfully locked, false if it
	 *         was already locked before
	 * @throws IOException
	 *             if something goes wrong on store level
	 */
	protected synchronized boolean lock(String path, String owner,
			boolean exclusive) throws IOException {

		Vector children = new Vector();
		children.add(new LockObject(path, owner, exclusive));

		// generate temporary locks
		Hashtable tempLockStore = new Hashtable();
		tempLockStore = generateLocks(getParentPath(path), children);
		// check if no new locks are allready existing
		if (checkLocks(tempLockStore, exclusive)) {

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
	private boolean checkLocks(Hashtable locks, boolean exclusive) {

		Enumeration tempKeys = locks.keys();

		while (tempKeys.hasMoreElements()) {
			String key = (String) tempKeys.nextElement();

			if (this.fLocks.containsKey(key)) {
				// a parent directory matches
				Vector children = (Vector) this.fLocks.get(key);
				Vector tempChildren = (Vector) locks.get(key);

				for (int i = 0; i < tempChildren.size(); i++) {
					LockObject tempChildrenElement = (LockObject) tempChildren
							.elementAt(i);
					String tempChildrenPath = tempChildrenElement.fPath;
					boolean tempChildrenExclusive = tempChildrenElement.fExclusive;
					if (this.fLocks.containsKey(tempChildrenPath)) {
						// one of the new children is already a key
						if (exclusive) {
							// exclusive (new)lock not possible -> old lock
							// inside
							return false;
						}
						// -shared (new) lock is possible if the old lock is
						// shared as well
						// -will be handled in the for clause below
					}

					for (int i2 = 0; i2 < children.size(); i2++) {
						LockObject childrenElement = (LockObject) children
								.elementAt(i2);
						String childrenPath = childrenElement.fPath;
						boolean childrenExclusive = childrenElement.fExclusive;
						if (childrenPath.equals(tempChildrenPath)) {
							// one of the new children already is an old child
							if (tempChildrenExclusive || childrenExclusive) {
								// the old lock or the new lock are exclusive
								return false;
							}
							// both locks are shared, which is no problem
						}
						// the new lock(s) have the same parent folder as the
						// old ones, but another name
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
	 *            a Vector with the children (LockObject) of parentPath
	 * @return a Hashtable with the generated Locks
	 * @throws IOException
	 */
	private Hashtable generateLocks(String parentPath, Vector childrenVector)
			throws IOException {

		Vector tempVector = new Vector(childrenVector);
		Hashtable tempLockStore = new Hashtable();
		String pathString = null;
		String owner = null;
		boolean exclusive = false;

		if (tempVector.size() > 0) {

			for (int childCount = 0; childCount < tempVector.size(); childCount++) {

				// initialize the local variables once
				LockObject tempLockObject = (LockObject) tempVector
						.elementAt(childCount);
				pathString = tempLockObject.fPath;
				owner = tempLockObject.fOwner;
				exclusive = tempLockObject.fExclusive;
				if (fStore.isFolder(pathString)) {
					tempLockObject.fIsFolder = true;
					tempVector.set(childCount, tempLockObject);

					// remember that this is a folder for later (for unlocking
					// deleted resource)

					String[] children = fStore.getChildrenNames(pathString);
					Vector newPath = new Vector(children.length);
					for (int i = 0; i < children.length; i++) {
						newPath.add(new LockObject(pathString + "/"
								+ children[i], owner, exclusive));
					}
					tempLockStore.putAll(generateLocks(pathString, newPath));

				} else {
					// a file or not existant -> probably a new resource locked
					// before creating
					// do nothing special

					// or some weird thing that is neither a file nor an object
					// but still exists
				}

			}
			tempLockStore.put(parentPath, tempVector);
		}
		return tempLockStore;
	}

	/**
	 * unlocks the resource at "path" (and all subfolders if existing)<p/> does
	 * NOT check if the removal is allowed!
	 * 
	 * @param path
	 *            what resource to unlock
	 * @param owner
	 *            who wants to unlock
	 * @throws IOException
	 */
	protected synchronized void unlock(String path, String owner)
			throws IOException {

		String parentPath = getParentPath(path);
		if (this.fLocks.containsKey(parentPath)) {
			Vector children = (Vector) this.fLocks.get(parentPath);
			for (int i = 0; i < children.size(); i++) {
				LockObject tempChild = (LockObject) children.elementAt(i);
				if (tempChild.fPath.equals(path)) {
					if (tempChild.fOwner.equals(owner)) {
						if (tempChild.fIsFolder) {
							deleteLocks(path, owner);
						}
						children.remove(i);
						// i--;
						// because the indexes in the vector shifted at the
						// removal
						// unneccessary, we return anyway and don't have to care
						// for the indexes
						if (children.size() != 0) {
							this.fLocks.put(parentPath, children);
						} else {
							this.fLocks.remove(parentPath);
						}

						return;
						// we found the given element, only 1 can be targeted by
						// this method

					}// else: not the child that we want to unlock (wrong
						// owner)
				} // else: not the child that we want to unlock (wrong path)
			}
		}
		// else: there is no lock at that path. someone tried to unlock it
		// anyway. could point to a problem
	}

	/**
	 * 
	 * @param parentPath
	 *            the folder which contens should be unlocked
	 * @param owner
	 *            the owner of the locks to be deleted
	 */
	private void deleteLocks(String parentPath, String owner) {
		Vector children = (Vector) this.fLocks.get(parentPath);
		// Vector is null if the folder was empty
		if (children != null) {
			for (int i = 0; i < children.size(); i++) {
				LockObject tempChild = (LockObject) children.elementAt(i);
				if (tempChild.fOwner.equals(owner)) {
					if (tempChild.fIsFolder) {
						deleteLocks(tempChild.fPath, owner);
					}
					children.remove(i);
					i--;
					// because the indexes in the vector shifted at the removal
				}// else: not the child that we want to unlock (wrong owner)

			}
			if (children.size() != 0) {
				this.fLocks.put(parentPath, children);
			} else {
				this.fLocks.remove(parentPath);
			}
			// replacing the old children with a vector where the locks of owner
			// where removed

		} else {
			this.fLocks.remove(parentPath);
			// completely removing the lock because the folder is empty
			// (empty folders are locked with their parent path as key)
		}
	}

	private String getParentPath(String path) {
		int slash = path.lastIndexOf('/');
		if (slash != -1) {
			return path.substring(0, slash);
		}
		return null;
	}

	private class LockObject {

		String fPath;

		String fOwner;

		boolean fExclusive;

		boolean fIsFolder = false;

		LockObject(String path, String owner, boolean exclusive) {
			this.fPath = path;
			this.fOwner = owner;
			this.fExclusive = exclusive;
		}
	}
}
