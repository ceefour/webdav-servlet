package net.sf.webdav.methods;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.webdav.ITransaction;
import net.sf.webdav.IWebDAVStore;
import net.sf.webdav.StoredObject;
import net.sf.webdav.WebDAVStatus;
import net.sf.webdav.locking.LockedObject;
import net.sf.webdav.locking.ResourceLocks;
import net.sf.webdav.testutil.MockTest;

import org.jmock.Expectations;
import org.junit.BeforeClass;
import org.junit.Test;

public class DoDeleteTest extends MockTest {

	static IWebDAVStore mockStore;
	static HttpServletRequest mockReq;
	static HttpServletResponse mockRes;
	static ITransaction mockTransaction;
	static byte[] resourceContent = new byte[] { '<', 'h', 'e', 'l', 'l', 'o', '/', '>' };

	@BeforeClass
	public static void setUp() throws Exception {
		mockStore = _mockery.mock(IWebDAVStore.class);
		mockReq = _mockery.mock(HttpServletRequest.class);
		mockRes = _mockery.mock(HttpServletResponse.class);
		mockTransaction = _mockery.mock(ITransaction.class);
	}

	@Test
	public void testDeleteIfReadOnlyIsTrue() throws Exception {

		_mockery.checking(new Expectations() {
			{
				oneOf(mockRes).sendError(WebDAVStatus.SC_FORBIDDEN);
			}
		});

		ResourceLocks resLocks = new ResourceLocks();
		DoDelete doDelete = new DoDelete(mockStore, resLocks, readOnly);
		doDelete.execute(mockTransaction, mockReq, mockRes);

		_mockery.assertIsSatisfied();
	}

	@Test
	public void testDeleteFileIfObjectExists() throws Exception {

		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(AbstractMethod.ATTR_INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(sourceFilePath));

				oneOf(mockRes).setStatus(WebDAVStatus.SC_NO_CONTENT);

				StoredObject fileSo = initFileStoredObject(resourceContent);

				oneOf(mockStore).getStoredObject(mockTransaction, sourceFilePath);
				will(returnValue(fileSo));

				oneOf(mockStore).removeObject(mockTransaction, sourceFilePath);
			}
		});

		DoDelete doDelete = new DoDelete(mockStore, new ResourceLocks(), !readOnly);

		doDelete.execute(mockTransaction, mockReq, mockRes);

		_mockery.assertIsSatisfied();
	}

	@Test
	public void testDeleteFileIfObjectNotExists() throws Exception {

		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(AbstractMethod.ATTR_INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(sourceFilePath));

				oneOf(mockRes).setStatus(WebDAVStatus.SC_NO_CONTENT);

				StoredObject fileSo = null;

				oneOf(mockStore).getStoredObject(mockTransaction, sourceFilePath);
				will(returnValue(fileSo));

				oneOf(mockRes).sendError(WebDAVStatus.SC_NOT_FOUND);
			}
		});

		DoDelete doDelete = new DoDelete(mockStore, new ResourceLocks(), !readOnly);

		doDelete.execute(mockTransaction, mockReq, mockRes);

		_mockery.assertIsSatisfied();
	}

	@Test
	public void testDeleteFolderIfObjectExists() throws Exception {

		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(AbstractMethod.ATTR_INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(sourceCollectionPath));

				oneOf(mockRes).setStatus(WebDAVStatus.SC_NO_CONTENT);

				StoredObject folderSo = initFolderStoredObject();

				oneOf(mockStore).getStoredObject(mockTransaction, sourceCollectionPath);
				will(returnValue(folderSo));

				oneOf(mockStore).getChildrenNames(mockTransaction, sourceCollectionPath);
				will(returnValue(new String[] { "subFolder", "sourceFile" }));

				StoredObject fileSo = initFileStoredObject(resourceContent);

				oneOf(mockStore).getStoredObject(mockTransaction, sourceFilePath);
				will(returnValue(fileSo));

				oneOf(mockStore).removeObject(mockTransaction, sourceFilePath);

				StoredObject subFolderSo = initFolderStoredObject();

				oneOf(mockStore).getStoredObject(mockTransaction, sourceCollectionPath + "/subFolder");
				will(returnValue(subFolderSo));

				oneOf(mockStore).getChildrenNames(mockTransaction, sourceCollectionPath + "/subFolder");
				will(returnValue(new String[] { "fileInSubFolder" }));

				StoredObject fileInSubFolderSo = initFileStoredObject(resourceContent);

				oneOf(mockStore).getStoredObject(mockTransaction, sourceCollectionPath + "/subFolder/fileInSubFolder");
				will(returnValue(fileInSubFolderSo));

				oneOf(mockStore).removeObject(mockTransaction, sourceCollectionPath + "/subFolder/fileInSubFolder");

				oneOf(mockStore).removeObject(mockTransaction, sourceCollectionPath + "/subFolder");

				oneOf(mockStore).removeObject(mockTransaction, sourceCollectionPath);
			}
		});

		DoDelete doDelete = new DoDelete(mockStore, new ResourceLocks(), !readOnly);

		doDelete.execute(mockTransaction, mockReq, mockRes);

		_mockery.assertIsSatisfied();
	}

	@Test
	public void testDeleteFolderIfObjectNotExists() throws Exception {

		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(AbstractMethod.ATTR_INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(sourceCollectionPath));

				oneOf(mockRes).setStatus(WebDAVStatus.SC_NO_CONTENT);

				StoredObject folderSo = null;

				oneOf(mockStore).getStoredObject(mockTransaction, sourceCollectionPath);
				will(returnValue(folderSo));

				oneOf(mockRes).sendError(WebDAVStatus.SC_NOT_FOUND);
			}
		});

		DoDelete doDelete = new DoDelete(mockStore, new ResourceLocks(), !readOnly);

		doDelete.execute(mockTransaction, mockReq, mockRes);

		_mockery.assertIsSatisfied();
	}

	@Test
	public void testDeleteFileInFolder() throws Exception {

		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(AbstractMethod.ATTR_INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(sourceFilePath));

				oneOf(mockRes).setStatus(WebDAVStatus.SC_NO_CONTENT);

				StoredObject fileSo = initFileStoredObject(resourceContent);

				oneOf(mockStore).getStoredObject(mockTransaction, sourceFilePath);
				will(returnValue(fileSo));

				oneOf(mockStore).removeObject(mockTransaction, sourceFilePath);
			}
		});

		DoDelete doDelete = new DoDelete(mockStore, new ResourceLocks(), !readOnly);

		doDelete.execute(mockTransaction, mockReq, mockRes);

		_mockery.assertIsSatisfied();
	}

	@Test
	public void testDeleteFileInLockedFolderWithWrongLockToken() throws Exception {

		final String lockedFolderPath = "/lockedFolder";
		final String fileInLockedFolderPath = lockedFolderPath.concat("/fileInLockedFolder");

		String owner = new String("owner");
		ResourceLocks resLocks = new ResourceLocks();

		resLocks.lock(mockTransaction, lockedFolderPath, owner, true, -1, TEMP_TIMEOUT, !TEMPORARY);
		LockedObject lo = resLocks.getLockedObjectByPath(mockTransaction, lockedFolderPath);
		final String wrongLockToken = "(<opaquelocktoken:" + lo.getID() + "WRONG>)";

		final PrintWriter pw = new PrintWriter(tmpFolder + "/XMLTestFile");

		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(AbstractMethod.ATTR_INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(fileInLockedFolderPath));

				oneOf(mockReq).getHeader("If");
				will(returnValue(wrongLockToken));

				oneOf(mockRes).setStatus(WebDAVStatus.SC_LOCKED);

//				oneOf(mockReq).getRequestURI();
//				will(returnValue("http://foo.bar".concat(lockedFolderPath)));

//				oneOf(mockRes).getWriter();
//				will(returnValue(pw));

			}
		});

		DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);

		doDelete.execute(mockTransaction, mockReq, mockRes);

		_mockery.assertIsSatisfied();
	}

	@Test
	public void testDeleteFileInLockedFolderWithRightLockToken() throws Exception {

		final String path = "/lockedFolder/fileInLockedFolder";
		final String parentPath = "/lockedFolder";
		final String owner = new String("owner");
		ResourceLocks resLocks = new ResourceLocks();

		resLocks.lock(mockTransaction, parentPath, owner, true, -1, TEMP_TIMEOUT, !TEMPORARY);
		LockedObject lo = resLocks.getLockedObjectByPath(mockTransaction, "/lockedFolder");
		final String rightLockToken = "(<opaquelocktoken:" + lo.getID() + ">)";

		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(AbstractMethod.ATTR_INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(path));

				oneOf(mockReq).getHeader("If");
				will(returnValue(rightLockToken));

				oneOf(mockRes).setStatus(WebDAVStatus.SC_NO_CONTENT);

				StoredObject so = initFileStoredObject(resourceContent);

				oneOf(mockStore).getStoredObject(mockTransaction, path);
				will(returnValue(so));

				oneOf(mockStore).removeObject(mockTransaction, path);

			}
		});

		DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);

		doDelete.execute(mockTransaction, mockReq, mockRes);

		_mockery.assertIsSatisfied();
	}

	@Test
	public void testDeleteFileInFolderIfObjectNotExists() throws Exception {

		boolean readOnly = false;

		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(AbstractMethod.ATTR_INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue("/folder/file"));

				oneOf(mockRes).setStatus(WebDAVStatus.SC_NO_CONTENT);

				StoredObject nonExistingSo = null;

				oneOf(mockStore).getStoredObject(mockTransaction, "/folder/file");
				will(returnValue(nonExistingSo));

				oneOf(mockRes).sendError(WebDAVStatus.SC_NOT_FOUND);
			}
		});

		DoDelete doDelete = new DoDelete(mockStore, new ResourceLocks(), readOnly);

		doDelete.execute(mockTransaction, mockReq, mockRes);

		_mockery.assertIsSatisfied();
	}

}
