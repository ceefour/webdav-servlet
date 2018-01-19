package net.sf.webdav.methods;

import java.io.ByteArrayInputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.webdav.ITransaction;
import net.sf.webdav.IWebDAVStore;
import net.sf.webdav.StoredObject;
import net.sf.webdav.WebDAVStatus;
import net.sf.webdav.locking.ResourceLocks;
import net.sf.webdav.testutil.MockTest;
import net.sf.webdav.util.URLUtil;

import org.jmock.Expectations;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.DelegatingServletInputStream;

public class DoMoveTest extends MockTest {

	static IWebDAVStore mockStore;
	static HttpServletRequest mockReq;
	static HttpServletResponse mockRes;
	static ITransaction mockTransaction;
	static byte[] resourceContent = new byte[] { '<', 'h', 'e', 'l', 'l', 'o', '/', '>' };
	static ByteArrayInputStream bais = new ByteArrayInputStream(resourceContent);
	static DelegatingServletInputStream dsis = new DelegatingServletInputStream(bais);

	static final String tmpFolder = "/tmp/tests";

	static final String sourceCollectionPath = tmpFolder + "/sourceFolder";
	static final String destCollectionPath = tmpFolder + "/destFolder";
	static final String sourceFilePath = sourceCollectionPath + "/sourceFile";
	static final String destFilePath = destCollectionPath + "/destFile";

	static final String overwritePath = destCollectionPath + "/sourceFolder";

	@BeforeClass
	public static void setUp() throws Exception {
		mockStore = _mockery.mock(IWebDAVStore.class);
		mockReq = _mockery.mock(HttpServletRequest.class);
		mockRes = _mockery.mock(HttpServletResponse.class);
		mockTransaction = _mockery.mock(ITransaction.class);

	}

	@Test
	public void testMovingOfFileOrFolderIfReadOnlyIsTrue() throws Exception {

		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(AbstractMethod.ATTR_INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(sourceFilePath));
				
				oneOf(mockReq).getHeader(AbstractMethod.HEADER_DESTINATION);
				will(returnValue(destFilePath));
				
				oneOf(mockRes).sendError(WebDAVStatus.SC_FORBIDDEN);
			}
		});

		ResourceLocks resLocks = new ResourceLocks();
		DoDelete doDelete = new DoDelete(mockStore, resLocks, readOnly);
		DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, readOnly);

		DoMove doMove = new DoMove(resLocks, doDelete, doCopy, readOnly);

		doMove.execute(mockTransaction, mockReq, mockRes);

		_mockery.assertIsSatisfied();
	}

	@Test
	public void testMovingOfaFileIfDestinationNotPresent() throws Exception {

		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(AbstractMethod.ATTR_INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(sourceFilePath));

				exactly(2).of(mockReq).getHeader(AbstractMethod.HEADER_DESTINATION);
				will(returnValue(destFilePath));

				oneOf(mockReq).getServerName();
				will(returnValue("serverName"));

				oneOf(mockReq).getContextPath();
				will(returnValue(""));

				oneOf(mockReq).getPathInfo();
				will(returnValue(destFilePath));

				oneOf(mockReq).getServletPath();
				will(returnValue("/servletPath"));

				oneOf(mockReq).getAttribute(AbstractMethod.ATTR_INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(sourceFilePath));

				oneOf(mockReq).getHeader(AbstractMethod.HEADER_OVERWRITE);
				will(returnValue("F"));

				StoredObject sourceFileSo = initFileStoredObject(resourceContent);

				oneOf(mockStore).getStoredObject(mockTransaction, URLUtil.getCleanPath(sourceFilePath));
				will(returnValue(sourceFileSo));

				StoredObject destFileSo = null;

				oneOf(mockStore).getStoredObject(mockTransaction, URLUtil.getCleanPath(destFilePath));
				will(returnValue(destFileSo));

				oneOf(mockRes).setStatus(WebDAVStatus.SC_CREATED);

				oneOf(mockStore).getStoredObject(mockTransaction, URLUtil.getCleanPath(sourceFilePath));
				will(returnValue(sourceFileSo));

				oneOf(mockStore).createResource(mockTransaction, URLUtil.getCleanPath(destFilePath));

				oneOf(mockStore).getResourceContent(mockTransaction, URLUtil.getCleanPath(sourceFilePath));
				will(returnValue(dsis));

				oneOf(mockStore).setResourceContent(mockTransaction, URLUtil.getCleanPath(destFilePath), dsis, null, null);
				will(returnValue(8L));

				destFileSo = initFileStoredObject(resourceContent);

				oneOf(mockStore).getStoredObject(mockTransaction, URLUtil.getCleanPath(destFilePath));
				will(returnValue(destFileSo));

				oneOf(mockRes).setStatus(WebDAVStatus.SC_NO_CONTENT);

				oneOf(mockStore).getStoredObject(mockTransaction, URLUtil.getCleanPath(sourceFilePath));
				will(returnValue(sourceFileSo));

				oneOf(mockStore).removeObject(mockTransaction, URLUtil.getCleanPath(sourceFilePath));
			}
		});

		ResourceLocks resLocks = new ResourceLocks();
		DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);
		DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !readOnly);

		DoMove doMove = new DoMove(resLocks, doDelete, doCopy, !readOnly);

		doMove.execute(mockTransaction, mockReq, mockRes);

		_mockery.assertIsSatisfied();
	}

	@Test
	public void testMovingOfaFileIfDestinationIsPresentAndOverwriteFalse() throws Exception {

		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(AbstractMethod.ATTR_INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(sourceFilePath));

				exactly(2).of(mockReq).getHeader(AbstractMethod.HEADER_DESTINATION);
				will(returnValue(destFilePath));

				oneOf(mockReq).getServerName();
				will(returnValue("server_name"));

				oneOf(mockReq).getContextPath();
				will(returnValue(""));

				oneOf(mockReq).getPathInfo();
				will(returnValue(destFilePath));

				oneOf(mockReq).getServletPath();
				will(returnValue("servlet_path"));

				oneOf(mockReq).getAttribute(AbstractMethod.ATTR_INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(sourceFilePath));

				oneOf(mockReq).getHeader(AbstractMethod.HEADER_OVERWRITE);
				will(returnValue("F"));

				StoredObject sourceFileSo = initFileStoredObject(resourceContent);

				oneOf(mockStore).getStoredObject(mockTransaction, URLUtil.getCleanPath(sourceFilePath));
				will(returnValue(sourceFileSo));

				StoredObject destFileSo = initFileStoredObject(resourceContent);

				oneOf(mockStore).getStoredObject(mockTransaction, URLUtil.getCleanPath(destFilePath));
				will(returnValue(destFileSo));

				oneOf(mockRes).sendError(WebDAVStatus.SC_PRECONDITION_FAILED);

			}
		});

		ResourceLocks resLocks = new ResourceLocks();
		DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);
		DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !readOnly);

		DoMove doMove = new DoMove(resLocks, doDelete, doCopy, !readOnly);

		doMove.execute(mockTransaction, mockReq, mockRes);

		_mockery.assertIsSatisfied();
	}

	@Test
	public void testMovingOfaFileIfDestinationIsPresentAndOverwriteTrue() throws Exception {

		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(AbstractMethod.ATTR_INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(sourceFilePath));

				exactly(2).of(mockReq).getHeader(AbstractMethod.HEADER_DESTINATION);
				will(returnValue(destFilePath));

				oneOf(mockReq).getServerName();
				will(returnValue("server_name"));

				oneOf(mockReq).getContextPath();
				will(returnValue(""));

				oneOf(mockReq).getPathInfo();
				will(returnValue(destFilePath));

				oneOf(mockReq).getServletPath();
				will(returnValue("servlet_path"));

				oneOf(mockReq).getAttribute(AbstractMethod.ATTR_INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(sourceFilePath));

				oneOf(mockReq).getHeader(AbstractMethod.HEADER_OVERWRITE);
				will(returnValue("T"));

				StoredObject sourceFileSo = initFileStoredObject(resourceContent);

				oneOf(mockStore).getStoredObject(mockTransaction, URLUtil.getCleanPath(sourceFilePath));
				will(returnValue(sourceFileSo));

				StoredObject destFileSo = initFileStoredObject(resourceContent);

				oneOf(mockStore).getStoredObject(mockTransaction, URLUtil.getCleanPath(destFilePath));
				will(returnValue(destFileSo));

				oneOf(mockRes).setStatus(WebDAVStatus.SC_NO_CONTENT);

				oneOf(mockStore).getStoredObject(mockTransaction, URLUtil.getCleanPath(destFilePath));
				will(returnValue(destFileSo));

				oneOf(mockStore).removeObject(mockTransaction, URLUtil.getCleanPath(destFilePath));

				oneOf(mockStore).getStoredObject(mockTransaction, URLUtil.getCleanPath(sourceFilePath));
				will(returnValue(sourceFileSo));

				oneOf(mockStore).createResource(mockTransaction, URLUtil.getCleanPath(destFilePath));

				oneOf(mockStore).getResourceContent(mockTransaction, URLUtil.getCleanPath(sourceFilePath));
				will(returnValue(dsis));

				oneOf(mockStore).setResourceContent(mockTransaction, URLUtil.getCleanPath(destFilePath), dsis, null, null);
				will(returnValue(8L));

				oneOf(mockStore).getStoredObject(mockTransaction, URLUtil.getCleanPath(destFilePath));
				will(returnValue(destFileSo));

				oneOf(mockRes).setStatus(WebDAVStatus.SC_NO_CONTENT);

				oneOf(mockStore).getStoredObject(mockTransaction, URLUtil.getCleanPath(sourceFilePath));
				will(returnValue(sourceFileSo));

				oneOf(mockStore).removeObject(mockTransaction, URLUtil.getCleanPath(sourceFilePath));
			}
		});

		ResourceLocks resLocks = new ResourceLocks();
		DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);
		DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !readOnly);

		DoMove doMove = new DoMove(resLocks, doDelete, doCopy, !readOnly);

		doMove.execute(mockTransaction, mockReq, mockRes);

		_mockery.assertIsSatisfied();
	}

	@Test
	public void testMovingOfaFileIfSourceNotPresent() throws Exception {

		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(AbstractMethod.ATTR_INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(sourceFilePath));

				exactly(2).of(mockReq).getHeader(AbstractMethod.HEADER_DESTINATION);
				will(returnValue(destFilePath));

				oneOf(mockReq).getServerName();
				will(returnValue("server_name"));

				oneOf(mockReq).getContextPath();
				will(returnValue(""));

				oneOf(mockReq).getPathInfo();
				will(returnValue(destFilePath));

				oneOf(mockReq).getServletPath();
				will(returnValue("servlet_path"));

				oneOf(mockReq).getAttribute(AbstractMethod.ATTR_INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(sourceFilePath));

				oneOf(mockReq).getHeader(AbstractMethod.HEADER_OVERWRITE);
				will(returnValue("F"));

				StoredObject sourceFileSo = null;

				oneOf(mockStore).getStoredObject(mockTransaction, URLUtil.getCleanPath(sourceFilePath));
				will(returnValue(sourceFileSo));

				oneOf(mockRes).sendError(WebDAVStatus.SC_NOT_FOUND);
			}
		});

		ResourceLocks resLocks = new ResourceLocks();
		DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);
		DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !readOnly);

		DoMove doMove = new DoMove(resLocks, doDelete, doCopy, !readOnly);

		doMove.execute(mockTransaction, mockReq, mockRes);

		_mockery.assertIsSatisfied();
	}

	@Test
	public void testMovingIfSourcePathEqualsDestinationPath() throws Exception {

		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(AbstractMethod.ATTR_INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(sourceFilePath));

				exactly(2).of(mockReq).getHeader(AbstractMethod.HEADER_DESTINATION);
				will(returnValue(sourceFilePath));

				oneOf(mockReq).getServerName();
				will(returnValue("server_name"));

				oneOf(mockReq).getContextPath();
				will(returnValue(""));

				oneOf(mockReq).getPathInfo();
				will(returnValue(sourceFilePath));

				oneOf(mockReq).getServletPath();
				will(returnValue("servlet_path"));

				oneOf(mockReq).getAttribute(AbstractMethod.ATTR_INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(sourceFilePath));

				oneOf(mockRes).sendError(WebDAVStatus.SC_FORBIDDEN);
			}
		});

		ResourceLocks resLocks = new ResourceLocks();
		DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);
		DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !readOnly);

		DoMove doMove = new DoMove(resLocks, doDelete, doCopy, !readOnly);

		doMove.execute(mockTransaction, mockReq, mockRes);

		_mockery.assertIsSatisfied();
	}

	@Test
	public void testMovingOfaCollectionIfDestinationIsNotPresent() throws Exception {

		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(AbstractMethod.ATTR_INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(sourceCollectionPath));

				exactly(2).of(mockReq).getHeader(AbstractMethod.HEADER_DESTINATION);
				will(returnValue(destCollectionPath));

				oneOf(mockReq).getServerName();
				will(returnValue("server_name"));

				oneOf(mockReq).getContextPath();
				will(returnValue(""));

				oneOf(mockReq).getPathInfo();
				will(returnValue(destCollectionPath));

				oneOf(mockReq).getServletPath();
				will(returnValue("servlet_path"));

				oneOf(mockReq).getAttribute(AbstractMethod.ATTR_INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(sourceCollectionPath));

				oneOf(mockReq).getHeader(AbstractMethod.HEADER_OVERWRITE);
				will(returnValue("F"));

				StoredObject sourceCollectionSo = initFolderStoredObject();

				oneOf(mockStore).getStoredObject(mockTransaction, URLUtil.getCleanPath(sourceCollectionPath));
				will(returnValue(sourceCollectionSo));

				StoredObject destCollectionSo = null;

				oneOf(mockStore).getStoredObject(mockTransaction, URLUtil.getCleanPath(destCollectionPath));
				will(returnValue(destCollectionSo));

				oneOf(mockRes).setStatus(WebDAVStatus.SC_CREATED);

				oneOf(mockStore).getStoredObject(mockTransaction, URLUtil.getCleanPath(sourceCollectionPath));
				will(returnValue(sourceCollectionSo));

				oneOf(mockStore).createFolder(mockTransaction, URLUtil.getCleanPath(destCollectionPath));

				oneOf(mockReq).getHeader(AbstractMethod.HEADER_DEPTH);
				will(returnValue(null));

				String[] sourceChildren = new String[] { "sourceFile" };

				oneOf(mockStore).getChildrenNames(mockTransaction, URLUtil.getCleanPath(sourceCollectionPath));
				will(returnValue(sourceChildren));

				StoredObject sourceFileSo = initFileStoredObject(resourceContent);

				oneOf(mockStore).getStoredObject(mockTransaction, URLUtil.getCleanPath(sourceCollectionPath,"/sourceFile"));
				will(returnValue(sourceFileSo));

				oneOf(mockStore).createResource(mockTransaction, URLUtil.getCleanPath(destCollectionPath,"/sourceFile"));

				oneOf(mockStore).getResourceContent(mockTransaction, URLUtil.getCleanPath(sourceCollectionPath,"/sourceFile"));
				will(returnValue(dsis));

				oneOf(mockStore).setResourceContent(mockTransaction, URLUtil.getCleanPath(destCollectionPath,"/sourceFile"), dsis, null,
						null);
				will(returnValue(8L));

				StoredObject movedSo = initFileStoredObject(resourceContent);

				oneOf(mockStore).getStoredObject(mockTransaction, URLUtil.getCleanPath(destCollectionPath,"/sourceFile"));
				will(returnValue(movedSo));

				oneOf(mockRes).setStatus(WebDAVStatus.SC_NO_CONTENT);

				oneOf(mockStore).getStoredObject(mockTransaction, URLUtil.getCleanPath(sourceCollectionPath));
				will(returnValue(sourceCollectionSo));

				sourceChildren = new String[] { "sourceFile" };

				oneOf(mockStore).getChildrenNames(mockTransaction, URLUtil.getCleanPath(sourceCollectionPath));
				will(returnValue(sourceChildren));

				oneOf(mockStore).getStoredObject(mockTransaction, URLUtil.getCleanPath(sourceFilePath));
				will(returnValue(sourceFileSo));

				oneOf(mockStore).removeObject(mockTransaction, URLUtil.getCleanPath(sourceFilePath));

				oneOf(mockStore).removeObject(mockTransaction, URLUtil.getCleanPath(sourceCollectionPath));
			}
		});

		ResourceLocks resLocks = new ResourceLocks();
		DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);
		DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !readOnly);

		DoMove doMove = new DoMove(resLocks, doDelete, doCopy, !readOnly);

		doMove.execute(mockTransaction, mockReq, mockRes);

		_mockery.assertIsSatisfied();
	}

	@Test
	public void testMovingOfaCollectionIfDestinationIsPresentAndOverwriteFalse() throws Exception {

		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(AbstractMethod.ATTR_INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(sourceCollectionPath));

				exactly(2).of(mockReq).getHeader(AbstractMethod.HEADER_DESTINATION);
				will(returnValue(destCollectionPath));

				oneOf(mockReq).getServerName();
				will(returnValue("server_name"));

				oneOf(mockReq).getContextPath();
				will(returnValue(""));

				oneOf(mockReq).getPathInfo();
				will(returnValue(destCollectionPath));

				oneOf(mockReq).getServletPath();
				will(returnValue("servlet_path"));

				oneOf(mockReq).getAttribute(AbstractMethod.ATTR_INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(sourceCollectionPath));

				oneOf(mockReq).getHeader(AbstractMethod.HEADER_OVERWRITE);
				will(returnValue("F"));

				StoredObject sourceCollectionSo = initFolderStoredObject();

				oneOf(mockStore).getStoredObject(mockTransaction, URLUtil.getCleanPath(sourceCollectionPath));
				will(returnValue(sourceCollectionSo));

				StoredObject destCollectionSo = initFolderStoredObject();

				oneOf(mockStore).getStoredObject(mockTransaction, URLUtil.getCleanPath(destCollectionPath));
				will(returnValue(destCollectionSo));

				oneOf(mockRes).sendError(WebDAVStatus.SC_PRECONDITION_FAILED);
			}
		});

		ResourceLocks resLocks = new ResourceLocks();
		DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);
		DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !readOnly);

		DoMove doMove = new DoMove(resLocks, doDelete, doCopy, !readOnly);

		doMove.execute(mockTransaction, mockReq, mockRes);

		_mockery.assertIsSatisfied();
	}

	@Test
	public void testMovingOfaCollectionIfDestinationIsPresentAndOverwriteTrue() throws Exception {

		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(AbstractMethod.ATTR_INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(sourceCollectionPath));

				exactly(2).of(mockReq).getHeader(AbstractMethod.HEADER_DESTINATION);
				will(returnValue(overwritePath));

				oneOf(mockReq).getServerName();
				will(returnValue("server_name"));

				oneOf(mockReq).getContextPath();
				will(returnValue(""));

				oneOf(mockReq).getPathInfo();
				will(returnValue(overwritePath));

				oneOf(mockReq).getServletPath();
				will(returnValue("servlet_path"));

				oneOf(mockReq).getAttribute(AbstractMethod.ATTR_INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(sourceCollectionPath));

				oneOf(mockReq).getHeader(AbstractMethod.HEADER_OVERWRITE);
				will(returnValue("T"));

				StoredObject sourceCollectionSo = initFolderStoredObject();

				oneOf(mockStore).getStoredObject(mockTransaction, URLUtil.getCleanPath(sourceCollectionPath));
				will(returnValue(sourceCollectionSo));

				StoredObject destCollectionSo = initFolderStoredObject();

				oneOf(mockStore).getStoredObject(mockTransaction, URLUtil.getCleanPath(overwritePath));
				will(returnValue(destCollectionSo));

				oneOf(mockRes).setStatus(WebDAVStatus.SC_NO_CONTENT);

				oneOf(mockStore).getStoredObject(mockTransaction, URLUtil.getCleanPath(overwritePath));
				will(returnValue(destCollectionSo));

				oneOf(mockStore).getChildrenNames(mockTransaction, URLUtil.getCleanPath(overwritePath));
				will(returnValue(destChildren));

				StoredObject destFileSo = initFileStoredObject(resourceContent);

				oneOf(mockStore).getStoredObject(mockTransaction, URLUtil.getCleanPath(overwritePath,"/destFile"));
				will(returnValue(destFileSo));

				oneOf(mockStore).removeObject(mockTransaction, URLUtil.getCleanPath(overwritePath,"/destFile"));

				oneOf(mockStore).removeObject(mockTransaction, URLUtil.getCleanPath(overwritePath));

				oneOf(mockStore).getStoredObject(mockTransaction, URLUtil.getCleanPath(sourceCollectionPath));
				will(returnValue(sourceCollectionSo));

				oneOf(mockStore).createFolder(mockTransaction, URLUtil.getCleanPath(overwritePath));

				oneOf(mockReq).getHeader(AbstractMethod.HEADER_DEPTH);
				will(returnValue(null));

				oneOf(mockStore).getChildrenNames(mockTransaction, URLUtil.getCleanPath(sourceCollectionPath));
				will(returnValue(sourceChildren));

				StoredObject sourceFileSo = initFileStoredObject(resourceContent);

				oneOf(mockStore).getStoredObject(mockTransaction, URLUtil.getCleanPath(sourceFilePath));
				will(returnValue(sourceFileSo));

				oneOf(mockStore).createResource(mockTransaction, URLUtil.getCleanPath(overwritePath,"/sourceFile"));

				oneOf(mockStore).getResourceContent(mockTransaction, URLUtil.getCleanPath(sourceFilePath));
				will(returnValue(dsis));

				oneOf(mockStore).setResourceContent(mockTransaction, URLUtil.getCleanPath(overwritePath,"/sourceFile"), dsis, null, null);

				StoredObject movedSo = initFileStoredObject(resourceContent);

				oneOf(mockStore).getStoredObject(mockTransaction, URLUtil.getCleanPath(overwritePath,"/sourceFile"));
				will(returnValue(movedSo));

				oneOf(mockRes).setStatus(WebDAVStatus.SC_NO_CONTENT);

				oneOf(mockStore).getStoredObject(mockTransaction, URLUtil.getCleanPath(sourceCollectionPath));
				will(returnValue(sourceCollectionSo));

				sourceChildren = new String[] { "sourceFile" };

				oneOf(mockStore).getChildrenNames(mockTransaction, URLUtil.getCleanPath(sourceCollectionPath));
				will(returnValue(sourceChildren));

				oneOf(mockStore).getStoredObject(mockTransaction, URLUtil.getCleanPath(sourceFilePath));
				will(returnValue(sourceFileSo));

				oneOf(mockStore).removeObject(mockTransaction, URLUtil.getCleanPath(sourceFilePath));

				oneOf(mockStore).removeObject(mockTransaction, URLUtil.getCleanPath(sourceCollectionPath));
			}
		});

		ResourceLocks resLocks = new ResourceLocks();
		DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);
		DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !readOnly);

		DoMove doMove = new DoMove(resLocks, doDelete, doCopy, !readOnly);

		doMove.execute(mockTransaction, mockReq, mockRes);

		_mockery.assertIsSatisfied();
	}

}
