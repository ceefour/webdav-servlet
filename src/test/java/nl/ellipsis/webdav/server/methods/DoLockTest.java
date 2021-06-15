package nl.ellipsis.webdav.server.methods;

import java.io.ByteArrayInputStream;
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
import nl.ellipsis.webdav.server.testutil.MockTest;

import org.jmock.Expectations;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.DelegatingServletInputStream;

public class DoLockTest extends MockTest {

	static IWebDAVStore mockStore;
	static HttpServletRequest mockReq;
	static HttpServletResponse mockRes;
	static ITransaction mockTransaction;
	static IResourceLocks mockResourceLocks;

	static boolean exclusive = true;
	static String depthString = "-1";
	static int depth = -1;
	static String timeoutString = "10";

	@BeforeClass
	public static void setUp() throws Exception {
		mockStore = _mockery.mock(IWebDAVStore.class);
		mockReq = _mockery.mock(HttpServletRequest.class);
		mockRes = _mockery.mock(HttpServletResponse.class);
		mockTransaction = _mockery.mock(ITransaction.class);
		mockResourceLocks = _mockery.mock(IResourceLocks.class);
	}

	@Test
	public void testDoLockIfReadOnly() throws Exception {

		final String lockPath = "/aFileToLock";

		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(lockPath));
				
				oneOf(mockRes).sendError(HttpServletResponse.SC_FORBIDDEN);
			}
		});

		ResourceLocks resLocks = new ResourceLocks();

		DoLock doLock = new DoLock(mockStore, resLocks, readOnly);
		doLock.execute(mockTransaction, mockReq, mockRes);

		_mockery.assertIsSatisfied();
	}

	@Test
	public void testDoRefreshLockOnLockedResource() throws Exception {

		final String lockPath = "/aFileToLock";
		final String lockOwner = "owner";

		ResourceLocks resLocks = new ResourceLocks();
		resLocks.lock(mockTransaction, lockPath, lockOwner, exclusive, depth, TEMP_TIMEOUT, !TEMPORARY);

		LockedObject lo = resLocks.getLockedObjectByPath(mockTransaction, lockPath);
		String lockTokenString = lo.getID();
		final String lockToken = "(<opaquelocktoken:" + lockTokenString + ">)";

		final PrintWriter pw = new PrintWriter(tmpFolder + "/XMLTestFile");

		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(lockPath));

				oneOf(mockReq).getHeader(HttpHeaders.IF);
				will(returnValue(lockToken));

				oneOf(mockReq).getHeader(javax.ws.rs.core.HttpHeaders.USER_AGENT);
				will(returnValue("Goliath"));

				exactly(2).of(mockReq).getHeader(HttpHeaders.IF);
				will(returnValue(lockToken));

				oneOf(mockReq).getHeader(HttpHeaders.TIMEOUT);
				will(returnValue("Infinite"));

				oneOf(mockRes).setStatus(HttpServletResponse.SC_OK);

				oneOf(mockRes).setContentType("text/xml; charset=UTF-8");

				oneOf(mockRes).getWriter();
				will(returnValue(pw));

				oneOf(mockRes).addHeader("Lock-Token",
						lockToken.substring(lockToken.indexOf("(") + 1, lockToken.indexOf(")")));
			}
		});

		DoLock doLock = new DoLock(mockStore, resLocks, !readOnly);
		doLock.execute(mockTransaction, mockReq, mockRes);

		_mockery.assertIsSatisfied();
	}

	@Test
	public void testDoExclusiveLockOnResource() throws Exception {

		final String lockPath = "/aFileToLock";

		ResourceLocks resLocks = new ResourceLocks();
		final PrintWriter pw = new PrintWriter(tmpFolder + "/XMLTestFile");

		final ByteArrayInputStream baisExclusive = new ByteArrayInputStream(exclusiveLockRequestByteArray);
		final DelegatingServletInputStream dsisExclusive = new DelegatingServletInputStream(baisExclusive);

		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(lockPath));

				oneOf(mockReq).getHeader(javax.ws.rs.core.HttpHeaders.USER_AGENT);
				will(returnValue("Goliath"));

				oneOf(mockReq).getHeader(HttpHeaders.IF);
				will(returnValue(null));

				StoredObject so = initFileStoredObject(resourceContent);

				oneOf(mockStore).getStoredObject(mockTransaction, lockPath);
				will(returnValue(so));

				oneOf(mockReq).getInputStream();
				will(returnValue(dsisExclusive));

				oneOf(mockReq).getHeader(HttpHeaders.DEPTH);
				will(returnValue(depthString));

				oneOf(mockReq).getHeader(HttpHeaders.TIMEOUT);
				will(returnValue(timeoutString));

				oneOf(mockRes).setStatus(HttpServletResponse.SC_OK);

				oneOf(mockRes).setContentType("text/xml; charset=UTF-8");

				oneOf(mockRes).getWriter();
				will(returnValue(pw));

				// addHeader("Lock-Token", "(<opaquelocktoken:xxx-xxx-xxx>)")
				oneOf(mockRes).addHeader(with(any(String.class)), with(any(String.class)));
			}
		});

		DoLock doLock = new DoLock(mockStore, resLocks, !readOnly);
		doLock.execute(mockTransaction, mockReq, mockRes);

		_mockery.assertIsSatisfied();
	}

	@Test
	public void testDoSharedLockOnResource() throws Exception {

		final String lockPath = "/aFileToLock";

		ResourceLocks resLocks = new ResourceLocks();
		final PrintWriter pw = new PrintWriter(tmpFolder + "/XMLTestFile");

		final ByteArrayInputStream baisShared = new ByteArrayInputStream(sharedLockRequestByteArray);
		final DelegatingServletInputStream dsisShared = new DelegatingServletInputStream(baisShared);

		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(lockPath));

				oneOf(mockReq).getHeader(javax.ws.rs.core.HttpHeaders.USER_AGENT);
				will(returnValue("Goliath"));

				oneOf(mockReq).getHeader(HttpHeaders.IF);
				will(returnValue(null));

				StoredObject so = initFileStoredObject(resourceContent);

				oneOf(mockStore).getStoredObject(mockTransaction, lockPath);
				will(returnValue(so));

				oneOf(mockReq).getInputStream();
				will(returnValue(dsisShared));

				oneOf(mockReq).getHeader(HttpHeaders.DEPTH);
				will(returnValue(depthString));

				oneOf(mockReq).getHeader(HttpHeaders.TIMEOUT);
				will(returnValue(timeoutString));

				oneOf(mockRes).setStatus(HttpServletResponse.SC_OK);

				oneOf(mockRes).setContentType("text/xml; charset=UTF-8");

				oneOf(mockRes).getWriter();
				will(returnValue(pw));

				// addHeader("Lock-Token", "(<opaquelocktoken:xxx-xxx-xxx>)")
				oneOf(mockRes).addHeader(with(any(String.class)), with(any(String.class)));
			}
		});

		DoLock doLock = new DoLock(mockStore, resLocks, !readOnly);
		doLock.execute(mockTransaction, mockReq, mockRes);

		_mockery.assertIsSatisfied();
	}

	@Test
	public void testDoExclusiveLockOnCollection() throws Exception {

		final String lockPath = "/aFolderToLock";

		ResourceLocks resLocks = new ResourceLocks();

		final PrintWriter pw = new PrintWriter(tmpFolder + "/XMLTestFile");

		final ByteArrayInputStream baisExclusive = new ByteArrayInputStream(exclusiveLockRequestByteArray);
		final DelegatingServletInputStream dsisExclusive = new DelegatingServletInputStream(baisExclusive);

		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(lockPath));

				oneOf(mockReq).getHeader(javax.ws.rs.core.HttpHeaders.USER_AGENT);
				will(returnValue("Goliath"));

				oneOf(mockReq).getHeader(HttpHeaders.IF);
				will(returnValue(null));

				StoredObject so = initFolderStoredObject();

				oneOf(mockStore).getStoredObject(mockTransaction, lockPath);
				will(returnValue(so));

				oneOf(mockReq).getInputStream();
				will(returnValue(dsisExclusive));

				oneOf(mockReq).getHeader(HttpHeaders.DEPTH);
				will(returnValue(depthString));

				oneOf(mockReq).getHeader(HttpHeaders.TIMEOUT);
				will(returnValue(timeoutString));

				oneOf(mockRes).setStatus(HttpServletResponse.SC_OK);

				oneOf(mockRes).setContentType("text/xml; charset=UTF-8");

				oneOf(mockRes).getWriter();
				will(returnValue(pw));

				// addHeader("Lock-Token", "(<opaquelocktoken:xxx-xxx-xxx>)")
				oneOf(mockRes).addHeader(with(any(String.class)), with(any(String.class)));
			}
		});

		DoLock doLock = new DoLock(mockStore, resLocks, !readOnly);
		doLock.execute(mockTransaction, mockReq, mockRes);

		_mockery.assertIsSatisfied();
	}

	@Test
	public void testDoSharedLockOnCollection() throws Exception {

		final String lockPath = "/aFolderToLock";

		ResourceLocks resLocks = new ResourceLocks();
		final PrintWriter pw = new PrintWriter(tmpFolder + "/XMLTestFile");

		final ByteArrayInputStream baisShared = new ByteArrayInputStream(sharedLockRequestByteArray);
		final DelegatingServletInputStream dsisShared = new DelegatingServletInputStream(baisShared);

		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(lockPath));

				oneOf(mockReq).getHeader(javax.ws.rs.core.HttpHeaders.USER_AGENT);
				will(returnValue("Goliath"));

				oneOf(mockReq).getHeader(HttpHeaders.IF);
				will(returnValue(null));

				StoredObject so = initFolderStoredObject();

				oneOf(mockStore).getStoredObject(mockTransaction, lockPath);
				will(returnValue(so));

				oneOf(mockReq).getInputStream();
				will(returnValue(dsisShared));

				oneOf(mockReq).getHeader(HttpHeaders.DEPTH);
				will(returnValue(depthString));

				oneOf(mockReq).getHeader(HttpHeaders.TIMEOUT);
				will(returnValue(timeoutString));

				oneOf(mockRes).setStatus(HttpServletResponse.SC_OK);

				oneOf(mockRes).setContentType("text/xml; charset=UTF-8");

				oneOf(mockRes).getWriter();
				will(returnValue(pw));

				// addHeader("Lock-Token", "(<opaquelocktoken:xxx-xxx-xxx>)")
				oneOf(mockRes).addHeader(with(any(String.class)), with(any(String.class)));
			}
		});

		DoLock doLock = new DoLock(mockStore, resLocks, !readOnly);
		doLock.execute(mockTransaction, mockReq, mockRes);

		_mockery.assertIsSatisfied();
	}

	@Test
	public void testDoLockNullResourceLock() throws Exception {

		final String parentPath = "/parentCollection";
		final String lockPath = parentPath.concat("/aNullResource");

		final PrintWriter pw = new PrintWriter(tmpFolder + "/XMLTestFile");

		final ByteArrayInputStream baisExclusive = new ByteArrayInputStream(exclusiveLockRequestByteArray);
		final DelegatingServletInputStream dsisExclusive = new DelegatingServletInputStream(baisExclusive);

		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(lockPath));

				LockedObject lockNullResourceLo = null;

				oneOf(mockResourceLocks).getLockedObjectByPath(mockTransaction, lockPath);
				will(returnValue(lockNullResourceLo));

				LockedObject parentLo = null;

				oneOf(mockResourceLocks).getLockedObjectByPath(mockTransaction, parentPath);
				will(returnValue(parentLo));

				oneOf(mockReq).getHeader(javax.ws.rs.core.HttpHeaders.USER_AGENT);
				will(returnValue("Goliath"));

				oneOf(mockResourceLocks).lock(with(any(ITransaction.class)), with(any(String.class)),
						with(any(String.class)), with(any(boolean.class)), with(any(int.class)), with(any(int.class)),
						with(any(boolean.class)));
				will(returnValue(true));

				oneOf(mockReq).getHeader(HttpHeaders.IF);
				will(returnValue(null));

				StoredObject lockNullResourceSo = null;

				oneOf(mockStore).getStoredObject(mockTransaction, lockPath);
				will(returnValue(lockNullResourceSo));

				StoredObject parentSo = null;

				oneOf(mockStore).getStoredObject(mockTransaction, parentPath);
				will(returnValue(parentSo));

				oneOf(mockStore).createFolder(mockTransaction, parentPath);

				oneOf(mockStore).getStoredObject(mockTransaction, lockPath);
				will(returnValue(lockNullResourceSo));

				oneOf(mockStore).createResource(mockTransaction, lockPath);

				oneOf(mockRes).setStatus(HttpServletResponse.SC_CREATED);

				lockNullResourceSo = initLockNullStoredObject();

				oneOf(mockStore).getStoredObject(mockTransaction, lockPath);
				will(returnValue(lockNullResourceSo));

				oneOf(mockReq).getInputStream();
				will(returnValue(dsisExclusive));

				oneOf(mockReq).getHeader(HttpHeaders.DEPTH);
				will(returnValue(("0")));

				oneOf(mockReq).getHeader(HttpHeaders.TIMEOUT);
				will(returnValue("Infinite"));

				ResourceLocks resLocks = ResourceLocks.class.newInstance();

				oneOf(mockResourceLocks).exclusiveLock(mockTransaction, lockPath, "I'am the Lock Owner", 0, AbstractMethod.getMaxTimeout());
				will(returnValue(true));

				lockNullResourceLo = initLockNullLockedObject(resLocks, lockPath);

				oneOf(mockResourceLocks).getLockedObjectByPath(mockTransaction, lockPath);
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
			}
		});

		DoLock doLock = new DoLock(mockStore, mockResourceLocks, !readOnly);
		doLock.execute(mockTransaction, mockReq, mockRes);

		_mockery.assertIsSatisfied();

	}

	@Test
	public void testLockOnMacOS() throws Exception {

		final String lockPath = "/aFileToLock";
		final String lockOwner = "owner";

		ResourceLocks resLocks = new ResourceLocks();
		resLocks.lock(mockTransaction, lockPath, lockOwner, false, depth, TEMP_TIMEOUT, !TEMPORARY);

		LockedObject lo = resLocks.getLockedObjectByPath(mockTransaction, lockPath);
		String lockTokenString = lo.getID();
		final String lockToken = "(<opaquelocktoken:" + lockTokenString + ">)";

		final PrintWriter pw = new PrintWriter(tmpFolder + "/XMLTestFile");

		_mockery.checking(new Expectations() {
			{
				exactly(2).of(mockReq).getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_PATH_INFO);
				will(returnValue(null));

				exactly(2).of(mockReq).getHeader(HttpHeaders.DEPTH);
				will(returnValue(depthString));

				exactly(2).of(mockReq).getPathInfo();
				will(returnValue(lockPath));

				exactly(2).of(mockReq).getHeader(HttpHeaders.IF);
				will(returnValue(null));

				exactly(2).of(mockReq).getHeader(javax.ws.rs.core.HttpHeaders.USER_AGENT);
				will(returnValue("Darwin"));

				exactly(2).of(mockReq).getHeader(HttpHeaders.TIMEOUT);
				will(returnValue("Infinite"));

				exactly(2).of(mockRes).setContentType("text/xml; charset=UTF-8");

				exactly(2).of(mockRes).getWriter();
				will(returnValue(pw));
				StoredObject so = initFileStoredObject(resourceContent);

				exactly(2).of(mockStore).getStoredObject(mockTransaction, lockPath);
				will(returnValue(so));

				exactly(2).of(mockRes).setStatus(HttpServletResponse.SC_OK);

				exactly(2).of(mockRes).addHeader("Lock-Token",
					lockToken.substring(lockToken.indexOf("(") + 1, lockToken.indexOf(")")));
			}
		});

		DoLock doLock = new DoLock(mockStore, resLocks, !readOnly);
		doLock.execute(mockTransaction, mockReq, mockRes);
		// calling lock twice should not return an error status
		doLock.execute(mockTransaction, mockReq, mockRes);

		_mockery.assertIsSatisfied();

	}
}
