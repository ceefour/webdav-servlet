package nl.ellipsis.webdav.server.methods;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.ellipsis.webdav.HttpHeaders;
import nl.ellipsis.webdav.server.ITransaction;
import nl.ellipsis.webdav.server.IWebDAVStore;
import nl.ellipsis.webdav.server.StoredObject;
import nl.ellipsis.webdav.server.WebDAVConstants;
import nl.ellipsis.webdav.server.locking.IResourceLocks;
import nl.ellipsis.webdav.server.locking.LockedObject;
import nl.ellipsis.webdav.server.locking.ResourceLocks;
import nl.ellipsis.webdav.server.methods.AbstractMethod;
import nl.ellipsis.webdav.server.methods.DoLock;
import nl.ellipsis.webdav.server.methods.DoPut;
import nl.ellipsis.webdav.server.testutil.MockTest;

import org.jmock.Expectations;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.http.HttpStatus;

public class DoPutTest extends MockTest {
	static IWebDAVStore mockStore;
	static HttpServletRequest mockReq;
	static HttpServletResponse mockRes;
	static IResourceLocks mockResourceLocks;
	static ITransaction mockTransaction;

	static String parentPath = "/parentCollection";
	static String path = parentPath.concat("/fileToPut");

	static boolean lazyFolderCreationOnPut = true;

	@BeforeClass
	public static void setUp() throws Exception {
		mockStore = _mockery.mock(IWebDAVStore.class);
		mockReq = _mockery.mock(HttpServletRequest.class);
		mockRes = _mockery.mock(HttpServletResponse.class);
		mockResourceLocks = _mockery.mock(IResourceLocks.class);
		mockTransaction = _mockery.mock(ITransaction.class);
	}

	@Test
	public void testDoPutIfReadOnlyTrue() throws Exception {

		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(path));
				
				oneOf(mockRes).sendError(HttpServletResponse.SC_FORBIDDEN);
			}
		});

		DoPut doPut = new DoPut(mockStore, new ResourceLocks(), readOnly, lazyFolderCreationOnPut);
		doPut.execute(mockTransaction, mockReq, mockRes);

		_mockery.assertIsSatisfied();
	}

	@Test
	public void testDoPutIfReadOnlyFalse() throws Exception {

		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(path));

				oneOf(mockReq).getHeader(javax.ws.rs.core.HttpHeaders.USER_AGENT);
				will(returnValue("Goliath agent"));

				StoredObject parentSo = initFolderStoredObject();

				oneOf(mockStore).getStoredObject(mockTransaction, parentPath);
				will(returnValue(parentSo));

				StoredObject fileSo = null;

				oneOf(mockStore).getStoredObject(mockTransaction, path);
				will(returnValue(fileSo));

				oneOf(mockStore).createResource(mockTransaction, path);

				oneOf(mockRes).setStatus(HttpServletResponse.SC_CREATED);

				oneOf(mockReq).getInputStream();
				will(returnValue(dsis));

				oneOf(mockStore).setResourceContent(mockTransaction, path, dsis, null, null);
				will(returnValue(8L));

				fileSo = initFileStoredObject(resourceContent);

				oneOf(mockStore).getStoredObject(mockTransaction, path);
				will(returnValue(fileSo));

				// User-Agent: Goliath --> dont add ContentLength
				// oneOf(mockRes).setContentLength(8);
			}
		});

		DoPut doPut = new DoPut(mockStore, new ResourceLocks(), !readOnly, lazyFolderCreationOnPut);
		doPut.execute(mockTransaction, mockReq, mockRes);

		_mockery.assertIsSatisfied();
	}

	/**
	 * https://tools.ietf.org/html/rfc4918#page-50 
	 * A PUT that would result in the creation of a resource without an appropriately scoped parent collection MUST fail with a 409 (Conflict).
	 * 
	 * @throws Exception
	 */
	@Test
	public void testDoPutIfLazyFolderCreationOnPutIsFalse() throws Exception {

		// final PrintWriter pw = new PrintWriter(tmpFolder + "/XMLTestFile");

		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(path));

				oneOf(mockReq).getHeader(javax.ws.rs.core.HttpHeaders.USER_AGENT);
				will(returnValue("Transmit agent"));

				StoredObject parentSo = null;

				oneOf(mockStore).getStoredObject(mockTransaction, parentPath);
				will(returnValue(parentSo));

				oneOf(mockRes).sendError(HttpServletResponse.SC_CONFLICT,HttpStatus.CONFLICT.getReasonPhrase());

//				oneOf(mockReq).getRequestURI();
//				will(returnValue("http://foo.bar".concat(path)));
//
//				oneOf(mockRes).getWriter();
//				will(returnValue(pw));

			}
		});

		DoPut doPut = new DoPut(mockStore, new ResourceLocks(), !readOnly, !lazyFolderCreationOnPut);
		doPut.execute(mockTransaction, mockReq, mockRes);

		_mockery.assertIsSatisfied();
	}

	@Test
	public void testDoPutIfLazyFolderCreationOnPutIsTrue() throws Exception {

		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(path));

				oneOf(mockReq).getHeader(javax.ws.rs.core.HttpHeaders.USER_AGENT);
				will(returnValue("WebDAVFS/1.5.0 (01500000) ....."));

				StoredObject parentSo = null;

				oneOf(mockStore).getStoredObject(mockTransaction, parentPath);
				will(returnValue(parentSo));

				oneOf(mockStore).createFolder(mockTransaction, parentPath);

				StoredObject fileSo = null;

				oneOf(mockStore).getStoredObject(mockTransaction, path);
				will(returnValue(fileSo));

				oneOf(mockStore).createResource(mockTransaction, path);

				oneOf(mockRes).setStatus(HttpServletResponse.SC_CREATED);

				oneOf(mockReq).getInputStream();
				will(returnValue(dsis));

				oneOf(mockStore).setResourceContent(mockTransaction, path, dsis, null, null);
				will(returnValue(8L));

				fileSo = initFileStoredObject(resourceContent);

				oneOf(mockStore).getStoredObject(mockTransaction, path);
				will(returnValue(fileSo));

			}
		});

		DoPut doPut = new DoPut(mockStore, new ResourceLocks(), !readOnly, lazyFolderCreationOnPut);
		doPut.execute(mockTransaction, mockReq, mockRes);

		_mockery.assertIsSatisfied();
	}

	@Test
	public void testDoPutIfParentPathIsResource() throws Exception {

		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(path));

				oneOf(mockReq).getHeader(javax.ws.rs.core.HttpHeaders.USER_AGENT);
				will(returnValue("WebDAVFS/1.5.0 (01500000) ....."));

				StoredObject parentSo = initFileStoredObject(resourceContent);

				oneOf(mockStore).getStoredObject(mockTransaction, parentPath);
				will(returnValue(parentSo));

				oneOf(mockRes).sendError(HttpServletResponse.SC_FORBIDDEN);
			}
		});

		DoPut doPut = new DoPut(mockStore, new ResourceLocks(), !readOnly, lazyFolderCreationOnPut);
		doPut.execute(mockTransaction, mockReq, mockRes);

		_mockery.assertIsSatisfied();
	}

	@Test
	public void testDoPutOnALockNullResource() throws Exception {

		final PrintWriter pw = new PrintWriter(tmpFolder + "/XMLTestFile");

		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(path));

				LockedObject lockNullResourceLo = null;

				oneOf(mockResourceLocks).getLockedObjectByPath(mockTransaction, path);
				will(returnValue(lockNullResourceLo));

				LockedObject parentLo = null;

				oneOf(mockResourceLocks).getLockedObjectByPath(mockTransaction, parentPath);
				will(returnValue(parentLo));

				oneOf(mockReq).getHeader(javax.ws.rs.core.HttpHeaders.USER_AGENT);
				will(returnValue("Transmit agent"));

				oneOf(mockResourceLocks).lock(with(any(ITransaction.class)), with(any(String.class)),
						with(any(String.class)), with(any(boolean.class)), with(any(int.class)), with(any(int.class)),
						with(any(boolean.class)));
				will(returnValue(true));

				oneOf(mockReq).getHeader(HttpHeaders.IF);
				will(returnValue(null));

				StoredObject lockNullResourceSo = null;

				oneOf(mockStore).getStoredObject(mockTransaction, path);
				will(returnValue(lockNullResourceSo));

				StoredObject parentSo = null;

				oneOf(mockStore).getStoredObject(mockTransaction, parentPath);
				will(returnValue(parentSo));

				oneOf(mockStore).createFolder(mockTransaction, parentPath);

				parentSo = initFolderStoredObject();

				oneOf(mockStore).getStoredObject(mockTransaction, path);
				will(returnValue(lockNullResourceSo));

				oneOf(mockStore).createResource(mockTransaction, path);

				lockNullResourceSo = initLockNullStoredObject();

				oneOf(mockRes).setStatus(HttpServletResponse.SC_NO_CONTENT);

				oneOf(mockStore).getStoredObject(mockTransaction, path);
				will(returnValue(lockNullResourceSo));

				oneOf(mockReq).getInputStream();
				will(returnValue(dsisExclusive));

				oneOf(mockReq).getHeader(HttpHeaders.DEPTH);
				will(returnValue(("0")));

				oneOf(mockReq).getHeader(HttpHeaders.TIMEOUT);
				will(returnValue("Infinite"));

				ResourceLocks resLocks = ResourceLocks.class.newInstance();

				oneOf(mockResourceLocks).exclusiveLock(mockTransaction, path, "I'am the Lock Owner", 0, 604800);
				will(returnValue(true));

				lockNullResourceLo = initLockNullLockedObject(resLocks, path);

				oneOf(mockResourceLocks).getLockedObjectByPath(mockTransaction, path);
				will(returnValue(lockNullResourceLo));

				oneOf(mockRes).setStatus(HttpServletResponse.SC_OK);

				oneOf(mockRes).setContentType("text/xml; charset=UTF-8");

				oneOf(mockRes).getWriter();
				will(returnValue(pw));

				String loId = null;
				if (lockNullResourceLo != null) {
					loId = lockNullResourceLo.getID();
				}
				final String lockToken = "<opaquelocktoken:" + loId + ">";

				oneOf(mockRes).addHeader("Lock-Token", lockToken);

				oneOf(mockResourceLocks).unlockTemporaryLockedObjects(with(any(ITransaction.class)),
						with(any(String.class)), with(any(String.class)));

				// // -----LOCK on a non-existing resource successful------
				// // --------now doPUT on the lock-null resource----------

				oneOf(mockReq).getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(path));

				oneOf(mockReq).getHeader(javax.ws.rs.core.HttpHeaders.USER_AGENT);
				will(returnValue("Transmit agent"));

				oneOf(mockResourceLocks).getLockedObjectByPath(mockTransaction, parentPath);
				will(returnValue(parentLo));

				oneOf(mockResourceLocks).getLockedObjectByPath(mockTransaction, path);
				will(returnValue(lockNullResourceLo));

				final String ifHeaderLockToken = "(<locktoken:" + loId + ">)";

				oneOf(mockReq).getHeader(HttpHeaders.IF);
				will(returnValue(ifHeaderLockToken));

				oneOf(mockResourceLocks).getLockedObjectByID(mockTransaction, loId);
				will(returnValue(lockNullResourceLo));

				oneOf(mockResourceLocks).lock(with(any(ITransaction.class)), with(any(String.class)),
						with(any(String.class)), with(any(boolean.class)), with(any(int.class)), with(any(int.class)),
						with(any(boolean.class)));
				will(returnValue(true));

				parentSo = initFolderStoredObject();

				oneOf(mockStore).getStoredObject(mockTransaction, parentPath);
				will(returnValue(parentSo));

				oneOf(mockStore).getStoredObject(mockTransaction, path);
				will(returnValue(lockNullResourceSo));

				oneOf(mockResourceLocks).getLockedObjectByPath(mockTransaction, path);
				will(returnValue(lockNullResourceLo));

				oneOf(mockReq).getHeader(HttpHeaders.IF);
				will(returnValue(ifHeaderLockToken));

				String[] owners = (lockNullResourceLo != null ? lockNullResourceLo.getOwner() : null);
				String owner = null;
				if (owners != null) {
					owner = owners[0];
				}

				oneOf(mockResourceLocks).unlock(mockTransaction, loId, owner);
				will(returnValue(true));

				oneOf(mockRes).setStatus(HttpServletResponse.SC_NO_CONTENT);

				oneOf(mockReq).getInputStream();
				will(returnValue(dsis));

				oneOf(mockStore).setResourceContent(mockTransaction, path, dsis, null, null);
				will(returnValue(8L));

				StoredObject newResourceSo = initFileStoredObject(resourceContent);

				oneOf(mockStore).getStoredObject(mockTransaction, path);
				will(returnValue(newResourceSo));

				oneOf(mockResourceLocks).unlockTemporaryLockedObjects(with(any(ITransaction.class)),
						with(any(String.class)), with(any(String.class)));
			}
		});

		DoLock doLock = new DoLock(mockStore, mockResourceLocks, !readOnly);
		doLock.execute(mockTransaction, mockReq, mockRes);

		DoPut doPut = new DoPut(mockStore, mockResourceLocks, !readOnly, lazyFolderCreationOnPut);
		doPut.execute(mockTransaction, mockReq, mockRes);

		_mockery.assertIsSatisfied();
	}
}
