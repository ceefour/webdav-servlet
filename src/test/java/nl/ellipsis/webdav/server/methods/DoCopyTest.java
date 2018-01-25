package nl.ellipsis.webdav.server.methods;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.ellipsis.webdav.server.ITransaction;
import nl.ellipsis.webdav.server.IWebDAVStore;
import nl.ellipsis.webdav.server.StoredObject;
import nl.ellipsis.webdav.server.WebDAVConstants;
import nl.ellipsis.webdav.server.WebDAVStatus;
import nl.ellipsis.webdav.server.locking.LockedObject;
import nl.ellipsis.webdav.server.locking.ResourceLocks;
import nl.ellipsis.webdav.server.methods.AbstractMethod;
import nl.ellipsis.webdav.server.methods.DoCopy;
import nl.ellipsis.webdav.server.methods.DoDelete;
import nl.ellipsis.webdav.server.testutil.MockTest;
import nl.ellipsis.webdav.server.util.URLUtil;

import org.jmock.Expectations;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.DelegatingServletInputStream;

public class DoCopyTest extends MockTest {

	static IWebDAVStore mockStore;
	static HttpServletRequest mockReq;
	static HttpServletResponse mockRes;
	static ITransaction mockTransaction;
	static ByteArrayInputStream bais = new ByteArrayInputStream(resourceContent);
	static DelegatingServletInputStream dsis = new DelegatingServletInputStream(bais);

	@BeforeClass
	public static void setUp() throws Exception {
		mockStore = _mockery.mock(IWebDAVStore.class);
		mockReq = _mockery.mock(HttpServletRequest.class);
		mockRes = _mockery.mock(HttpServletResponse.class);
		mockTransaction = _mockery.mock(ITransaction.class);
	}

	@Test
	public void testDoCopyIfReadOnly() throws Exception {

		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(sourceFilePath));

				oneOf(mockRes).sendError(WebDAVStatus.SC_FORBIDDEN);
			}
		});

		ResourceLocks resLocks = new ResourceLocks();
		DoDelete doDelete = new DoDelete(mockStore, resLocks, readOnly);

		DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, readOnly);
		doCopy.execute(mockTransaction, mockReq, mockRes);

		_mockery.assertIsSatisfied();
	}

	@Test
	public void testDoCopyOfLockNullResource() throws Exception {

		final String parentPath = "/lockedFolder";
		final String path = parentPath.concat("/nullFile");

		String owner = new String("owner");
		ResourceLocks resLocks = new ResourceLocks();
		resLocks.lock(mockTransaction, parentPath, owner, true, 1, TEMP_TIMEOUT, !TEMPORARY);
		
		final LockedObject lo = resLocks.getLockedObjectByPath(mockTransaction, parentPath);
		final String rightLockToken = "(<opaquelocktoken:" + lo.getID() + ">)";

		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(path));

				oneOf(mockReq).getHeader(WebDAVConstants.HttpHeader.DESTINATION);
				will(returnValue("/destination"));

				oneOf(mockReq).getServerName();
				will(returnValue("myServer"));

				oneOf(mockReq).getContextPath();
				will(returnValue(""));

				oneOf(mockReq).getPathInfo();
				will(returnValue("/destination"));

				oneOf(mockReq).getServletPath();
				will(returnValue("/servletPath"));

				oneOf(mockReq).getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(path));

				oneOf(mockReq).getHeader(WebDAVConstants.HttpHeader.IF);
				will(returnValue(rightLockToken));

				oneOf(mockReq).getHeader(WebDAVConstants.HttpHeader.OVERWRITE);
				will(returnValue("T"));

				StoredObject so = initLockNullStoredObject();

				oneOf(mockStore).getStoredObject(mockTransaction, path);
				will(returnValue(so));

				oneOf(mockRes).addHeader("Allow", "OPTIONS, MKCOL, PUT, PROPFIND, LOCK, UNLOCK");

				oneOf(mockRes).sendError(WebDAVStatus.SC_METHOD_NOT_ALLOWED);
			}
		});

		DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);

		DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !readOnly);
		doCopy.execute(mockTransaction, mockReq, mockRes);

		_mockery.assertIsSatisfied();
	}

	@Test
	public void testDoCopyIfParentIsLockedWithWrongLockToken() throws Exception {

		String owner = new String("owner");
		ResourceLocks resLocks = new ResourceLocks();
		resLocks.lock(mockTransaction, destCollectionPath, owner, true, 1, TEMP_TIMEOUT, !TEMPORARY);

		final LockedObject lo = resLocks.getLockedObjectByPath(mockTransaction, destCollectionPath);
		final String wrongLockToken = "(<opaquelocktoken:" + lo.getID() + "WRONG>)";

		final PrintWriter pw = new PrintWriter(tmpFolder + "/XMLTestFile");

		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(sourceFilePath));

				oneOf(mockReq).getHeader(WebDAVConstants.HttpHeader.DESTINATION);
				will(returnValue(destFilePath));

				oneOf(mockReq).getServerName();
				will(returnValue("myServer"));

				oneOf(mockReq).getContextPath();
				will(returnValue(""));

				oneOf(mockReq).getPathInfo();
				will(returnValue(destFilePath));

				oneOf(mockReq).getServletPath();
				will(returnValue("/servletPath"));

				oneOf(mockReq).getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(sourceFilePath));

				oneOf(mockReq).getHeader(WebDAVConstants.HttpHeader.IF);
				will(returnValue(wrongLockToken));
				
				oneOf(mockRes).setStatus(WebDAVStatus.SC_LOCKED);
			}
		});

		DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);

		DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !readOnly);
		doCopy.execute(mockTransaction, mockReq, mockRes);

		_mockery.assertIsSatisfied();
	}

	@Test
	public void testDoCopyIfParentIsLockedWithRightLockToken() throws Exception {

		String owner = new String("owner");
		ResourceLocks resLocks = new ResourceLocks();

		resLocks.lock(mockTransaction, destCollectionPath, owner, true, 1, TEMP_TIMEOUT, !TEMPORARY);

		final LockedObject lo = resLocks.getLockedObjectByPath(mockTransaction, destCollectionPath);
		final String rightLockToken = "(<opaquelocktoken:" + lo.getID() + ">)";

		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(sourceFilePath));

				oneOf(mockReq).getHeader(WebDAVConstants.HttpHeader.DESTINATION);
				will(returnValue(destFilePath));

				oneOf(mockReq).getServerName();
				will(returnValue("myServer"));

				oneOf(mockReq).getContextPath();
				will(returnValue(""));

				oneOf(mockReq).getPathInfo();
				will(returnValue(destFilePath));

				oneOf(mockReq).getServletPath();
				will(returnValue("/servletPath"));

				oneOf(mockReq).getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(sourceFilePath));

				oneOf(mockReq).getHeader(WebDAVConstants.HttpHeader.IF);
				will(returnValue(rightLockToken));

				oneOf(mockReq).getHeader(WebDAVConstants.HttpHeader.OVERWRITE);
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
				will(returnValue(resourceLength));

				destFileSo = initFileStoredObject(resourceContent);

				oneOf(mockStore).getStoredObject(mockTransaction, URLUtil.getCleanPath(destFilePath));
				will(returnValue(destFileSo));

			}
		});

		DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);

		DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !readOnly);
		doCopy.execute(mockTransaction, mockReq, mockRes);

		_mockery.assertIsSatisfied();
	}

	@Test
	public void testDoCopyIfDestinationPathInvalid() throws Exception {

		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(sourceFilePath));

				oneOf(mockReq).getHeader(WebDAVConstants.HttpHeader.DESTINATION);
				will(returnValue(null));

				oneOf(mockRes).sendError(WebDAVStatus.SC_BAD_REQUEST);
			}
		});

		ResourceLocks resLocks = new ResourceLocks();
		DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);

		DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !readOnly);
		doCopy.execute(mockTransaction, mockReq, mockRes);

		_mockery.assertIsSatisfied();

	}

	@Test
	public void testDoCopyIfSourceEqualsDestination() throws Exception {

		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(sourceFilePath));

				oneOf(mockReq).getHeader(WebDAVConstants.HttpHeader.DESTINATION);
				will(returnValue(sourceFilePath));

				oneOf(mockReq).getServerName();
				will(returnValue("serverName"));

				oneOf(mockReq).getContextPath();
				will(returnValue(""));

				oneOf(mockReq).getPathInfo();
				will(returnValue(sourceFilePath));

				oneOf(mockReq).getServletPath();
				will(returnValue("/servletPath"));

				oneOf(mockReq).getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(sourceFilePath));

				oneOf(mockRes).sendError(WebDAVStatus.SC_FORBIDDEN);

			}
		});

		ResourceLocks resLocks = new ResourceLocks();

		DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);

		DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !readOnly);
		doCopy.execute(mockTransaction, mockReq, mockRes);

		_mockery.assertIsSatisfied();
	}

	@Test
	public void testDoCopyFolderIfNoLocks() throws Exception {

		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(sourceCollectionPath));

				oneOf(mockReq).getHeader(WebDAVConstants.HttpHeader.DESTINATION);
				will(returnValue(destCollectionPath));

				oneOf(mockReq).getServerName();
				will(returnValue("serverName"));

				oneOf(mockReq).getContextPath();
				will(returnValue(""));

				oneOf(mockReq).getPathInfo();
				will(returnValue(destCollectionPath));

				oneOf(mockReq).getServletPath();
				will(returnValue("/servletPath"));

				oneOf(mockReq).getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(sourceCollectionPath));

				oneOf(mockReq).getHeader(WebDAVConstants.HttpHeader.OVERWRITE);
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

				oneOf(mockReq).getHeader(WebDAVConstants.HttpHeader.DEPTH);
				will(returnValue("-1"));

				sourceChildren = new String[] { "sourceFile" };

				oneOf(mockStore).getChildrenNames(mockTransaction, URLUtil.getCleanPath(sourceCollectionPath));
				will(returnValue(sourceChildren));

				StoredObject sourceFileSo = initFileStoredObject(resourceContent);

				oneOf(mockStore).getStoredObject(mockTransaction, URLUtil.getCleanPath(sourceFilePath));
				will(returnValue(sourceFileSo));

				oneOf(mockStore).createResource(mockTransaction, URLUtil.getCleanPath(destCollectionPath,"/sourceFile"));

				oneOf(mockStore).getResourceContent(mockTransaction, URLUtil.getCleanPath(sourceFilePath));
				will(returnValue(dsis));

				oneOf(mockStore).setResourceContent(mockTransaction, URLUtil.getCleanPath(destCollectionPath,"/sourceFile"), dsis, null,
						null);

				StoredObject destFileSo = initFileStoredObject(resourceContent);

				oneOf(mockStore).getStoredObject(mockTransaction, URLUtil.getCleanPath(destCollectionPath,"/sourceFile"));
				will(returnValue(destFileSo));

			}
		});

		ResourceLocks resLocks = new ResourceLocks();

		DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);

		DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !readOnly);
		doCopy.execute(mockTransaction, mockReq, mockRes);

		_mockery.assertIsSatisfied();
	}

	@Test
	public void testDoCopyIfSourceDoesntExist() throws Exception {

		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(sourceFilePath));

				oneOf(mockReq).getHeader(WebDAVConstants.HttpHeader.DESTINATION);
				will(returnValue(destFilePath));

				oneOf(mockReq).getServerName();
				will(returnValue("serverName"));

				oneOf(mockReq).getContextPath();
				will(returnValue(""));

				oneOf(mockReq).getPathInfo();
				will(returnValue(destFilePath));

				oneOf(mockReq).getServletPath();
				will(returnValue("/servletPath"));

				oneOf(mockReq).getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(sourceFilePath));

				oneOf(mockReq).getHeader(WebDAVConstants.HttpHeader.OVERWRITE);
				will(returnValue("F"));

				StoredObject notExistSo = null;

				oneOf(mockStore).getStoredObject(mockTransaction, URLUtil.getCleanPath(sourceFilePath));
				will(returnValue(notExistSo));

				oneOf(mockRes).sendError(WebDAVStatus.SC_NOT_FOUND);

			}
		});

		ResourceLocks resLocks = new ResourceLocks();

		DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);

		DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !readOnly);
		doCopy.execute(mockTransaction, mockReq, mockRes);

		_mockery.assertIsSatisfied();
	}

	@Test
	public void testDoCopyIfDestinationAlreadyExistsAndOverwriteTrue() throws Exception {

		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(sourceFilePath));

				StoredObject sourceSo = initFileStoredObject(resourceContent);

				oneOf(mockStore).getStoredObject(mockTransaction, URLUtil.getCleanPath(sourceFilePath));
				will(returnValue(sourceSo));

				oneOf(mockReq).getHeader(WebDAVConstants.HttpHeader.DESTINATION);
				will(returnValue(destFilePath));

				oneOf(mockReq).getServerName();
				will(returnValue("serverName"));

				oneOf(mockReq).getContextPath();
				will(returnValue(""));

				oneOf(mockReq).getPathInfo();
				will(returnValue("/folder/destFolder"));

				oneOf(mockReq).getServletPath();
				will(returnValue("/servletPath"));

				oneOf(mockReq).getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(sourceFilePath));

				StoredObject existingDestSo = initFileStoredObject(resourceContent);

				oneOf(mockStore).getStoredObject(mockTransaction, URLUtil.getCleanPath(destFilePath));
				will(returnValue(existingDestSo));

				oneOf(mockReq).getHeader(WebDAVConstants.HttpHeader.OVERWRITE);
				will(returnValue("T"));

				oneOf(mockRes).setStatus(WebDAVStatus.SC_NO_CONTENT);

				oneOf(mockStore).getStoredObject(mockTransaction, URLUtil.getCleanPath(destFilePath));
				will(returnValue(existingDestSo));

				oneOf(mockStore).removeObject(mockTransaction, URLUtil.getCleanPath(destFilePath));

				oneOf(mockStore).getStoredObject(mockTransaction, URLUtil.getCleanPath(sourceFilePath));
				will(returnValue(sourceSo));

				oneOf(mockStore).createResource(mockTransaction, URLUtil.getCleanPath(destFilePath));

				oneOf(mockStore).getResourceContent(mockTransaction, URLUtil.getCleanPath(sourceFilePath));
				will(returnValue(dsis));

				oneOf(mockStore).setResourceContent(mockTransaction, URLUtil.getCleanPath(destFilePath), dsis, null, null);

				oneOf(mockStore).getStoredObject(mockTransaction, URLUtil.getCleanPath(destFilePath));
				will(returnValue(existingDestSo));

			}
		});

		ResourceLocks resLocks = new ResourceLocks();
		DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);

		DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !readOnly);
		doCopy.execute(mockTransaction, mockReq, mockRes);

		_mockery.assertIsSatisfied();

	}

	@Test
	public void testDoCopyIfDestinationAlreadyExistsAndOverwriteFalse() throws Exception {

		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(sourceFilePath));

				StoredObject sourceSo = initFileStoredObject(resourceContent);

				oneOf(mockStore).getStoredObject(mockTransaction, URLUtil.getCleanPath(sourceFilePath));
				will(returnValue(sourceSo));

				oneOf(mockReq).getHeader(WebDAVConstants.HttpHeader.DESTINATION);
				will(returnValue("serverName".concat(destFilePath)));

				oneOf(mockReq).getServerName();
				will(returnValue("serverName"));

				oneOf(mockReq).getContextPath();
				will(returnValue(""));

				oneOf(mockReq).getPathInfo();
				will(returnValue(destFilePath));

				oneOf(mockReq).getServletPath();
				will(returnValue("/servletPath"));

				oneOf(mockReq).getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(sourceFilePath));

				StoredObject existingDestSo = initFileStoredObject(resourceContent);

				oneOf(mockStore).getStoredObject(mockTransaction, URLUtil.getCleanPath(destFilePath));
				will(returnValue(existingDestSo));

				oneOf(mockReq).getHeader(WebDAVConstants.HttpHeader.OVERWRITE);
				will(returnValue("F"));

				oneOf(mockRes).sendError(WebDAVStatus.SC_PRECONDITION_FAILED);

			}
		});

		ResourceLocks resLocks = new ResourceLocks();
		DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);

		DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !readOnly);
		doCopy.execute(mockTransaction, mockReq, mockRes);

		_mockery.assertIsSatisfied();

	}

	@Test
	public void testDoCopyIfOverwriteTrue() throws Exception {

		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(sourceFilePath));

				StoredObject sourceSo = initFileStoredObject(resourceContent);

				oneOf(mockStore).getStoredObject(mockTransaction, URLUtil.getCleanPath(sourceFilePath));
				will(returnValue(sourceSo));

				oneOf(mockReq).getHeader(WebDAVConstants.HttpHeader.DESTINATION);
				will(returnValue("http://destination:80/".concat(destFilePath)));

				oneOf(mockReq).getContextPath();
				will(returnValue("http://destination:80"));

				oneOf(mockReq).getPathInfo();
				will(returnValue(destCollectionPath));

				oneOf(mockReq).getServletPath();
				will(returnValue("http://destination:80"));

				oneOf(mockReq).getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_PATH_INFO);
				will(returnValue(sourceFilePath));

				oneOf(mockReq).getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_PATH_INFO);
				will(returnValue(sourceFilePath));

				oneOf(mockReq).getHeader(WebDAVConstants.HttpHeader.OVERWRITE);
				will(returnValue("T"));

				StoredObject destFileSo = initFileStoredObject(resourceContent);

				oneOf(mockStore).getStoredObject(mockTransaction, URLUtil.getCleanPath(destFilePath));
				will(returnValue(destFileSo));

				oneOf(mockRes).setStatus(WebDAVStatus.SC_NO_CONTENT);

				oneOf(mockStore).getStoredObject(mockTransaction, URLUtil.getCleanPath(destFilePath));
				will(returnValue(destFileSo));

				oneOf(mockStore).removeObject(mockTransaction, URLUtil.getCleanPath(destFilePath));

				oneOf(mockStore).getStoredObject(mockTransaction, URLUtil.getCleanPath(sourceFilePath));
				will(returnValue(sourceSo));

				oneOf(mockStore).createResource(mockTransaction, URLUtil.getCleanPath(destFilePath));

				oneOf(mockStore).getResourceContent(mockTransaction, URLUtil.getCleanPath(sourceFilePath));
				will(returnValue(dsis));

				oneOf(mockStore).setResourceContent(mockTransaction, URLUtil.getCleanPath(destFilePath), dsis, null, null);

				oneOf(mockStore).getStoredObject(mockTransaction, URLUtil.getCleanPath(destFilePath));
				will(returnValue(destFileSo));
			}
		});

		ResourceLocks resLocks = new ResourceLocks();
		DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);

		DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !readOnly);
		doCopy.execute(mockTransaction, mockReq, mockRes);

		_mockery.assertIsSatisfied();

	}

	@Test
	public void testDoCopyIfOverwriteFalse() throws Exception {

		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(sourceFilePath));

				StoredObject sourceSo = initFileStoredObject(resourceContent);

				oneOf(mockStore).getStoredObject(mockTransaction, URLUtil.getCleanPath(sourceFilePath));
				will(returnValue(sourceSo));

				oneOf(mockReq).getHeader(WebDAVConstants.HttpHeader.DESTINATION);
				will(returnValue("http://destination:80/".concat(destCollectionPath)));

				oneOf(mockReq).getContextPath();
				will(returnValue("http://destination:80"));

				oneOf(mockReq).getPathInfo();
				will(returnValue(sourceFilePath));

				oneOf(mockReq).getServletPath();
				will(returnValue("http://destination:80"));

				oneOf(mockReq).getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_PATH_INFO);
				will(returnValue(sourceFilePath));

				oneOf(mockReq).getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_PATH_INFO);
				will(returnValue(sourceFilePath));

				oneOf(mockReq).getHeader(WebDAVConstants.HttpHeader.OVERWRITE);
				will(returnValue("F"));

				StoredObject existingDestSo = initFolderStoredObject();

				oneOf(mockStore).getStoredObject(mockTransaction, URLUtil.getCleanPath(destCollectionPath));
				will(returnValue(existingDestSo));

				oneOf(mockRes).sendError(WebDAVStatus.SC_PRECONDITION_FAILED);
			}
		});

		ResourceLocks resLocks = new ResourceLocks();
		DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);

		DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !readOnly);
		doCopy.execute(mockTransaction, mockReq, mockRes);

		_mockery.assertIsSatisfied();

	}
}
