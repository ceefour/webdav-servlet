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
import nl.ellipsis.webdav.server.methods.DoUnlock;
import nl.ellipsis.webdav.server.testutil.MockTest;

import org.jmock.Expectations;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.DelegatingServletInputStream;

public class DoUnlockTest extends MockTest {

	static IWebDAVStore mockStore;
	static HttpServletRequest mockReq;
	static HttpServletResponse mockRes;
	static ITransaction mockTransaction;
	static IResourceLocks mockResourceLocks;

	static boolean exclusive = true;

	@BeforeClass
	public static void setUp() throws Exception {
		mockStore = _mockery.mock(IWebDAVStore.class);
		mockReq = _mockery.mock(HttpServletRequest.class);
		mockRes = _mockery.mock(HttpServletResponse.class);
		mockTransaction = _mockery.mock(ITransaction.class);
		mockResourceLocks = _mockery.mock(IResourceLocks.class);
	}

	@Test
	public void testDoUnlockIfReadOnly() throws Exception {
		final String roPath = "/readOnlyResource";
		
		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(roPath));
				
				oneOf(mockRes).sendError(HttpServletResponse.SC_FORBIDDEN);
			}
		});

		DoUnlock doUnlock = new DoUnlock(mockStore, new ResourceLocks(), readOnly);

		doUnlock.execute(mockTransaction, mockReq, mockRes);

		_mockery.assertIsSatisfied();
	}

	@Test
	public void testDoUnlockaLockedResourceWithRightLockToken() throws Exception {

		final String lockPath = "/lockedResource";
		final String lockOwner = "theOwner";

		ResourceLocks resLocks = new ResourceLocks();
		resLocks.lock(mockTransaction, lockPath, lockOwner, exclusive, 0, TEMP_TIMEOUT, !TEMPORARY);

		LockedObject lo = resLocks.getLockedObjectByPath(mockTransaction, lockPath);
		final String loID = lo.getID();
		final String lockToken = "<opaquelocktoken:".concat(loID).concat(">");

		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(lockPath));

				oneOf(mockReq).getHeader(HttpHeaders.LOCK_TOKEN);
				will(returnValue(lockToken));

				StoredObject lockedSo = initFileStoredObject(resourceContent);

				oneOf(mockStore).getStoredObject(mockTransaction, lockPath);
				will(returnValue(lockedSo));

				oneOf(mockRes).setStatus(HttpServletResponse.SC_NO_CONTENT);
			}
		});

		DoUnlock doUnlock = new DoUnlock(mockStore, resLocks, !readOnly);

		doUnlock.execute(mockTransaction, mockReq, mockRes);

		_mockery.assertIsSatisfied();
	}

	@Test
	public void testDoUnlockaLockedResourceWithWrongLockToken() throws Exception {

		final String lockPath = "/lockedResource";
		final String lockOwner = "theOwner";

		ResourceLocks resLocks = new ResourceLocks();
		resLocks.lock(mockTransaction, lockPath, lockOwner, exclusive, 0, TEMP_TIMEOUT, !TEMPORARY);

		LockedObject lo = resLocks.getLockedObjectByPath(mockTransaction, lockPath);
		final String loID = lo.getID();
		final String lockToken = "<opaquelocktoken:".concat(loID).concat("WRONG>");

		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(lockPath));

				oneOf(mockReq).getHeader(HttpHeaders.LOCK_TOKEN);
				will(returnValue(lockToken));

				oneOf(mockRes).sendError(HttpServletResponse.SC_BAD_REQUEST);
			}
		});

		DoUnlock doUnlock = new DoUnlock(mockStore, resLocks, !readOnly);
		doUnlock.execute(mockTransaction, mockReq, mockRes);

		_mockery.assertIsSatisfied();
	}

	@Test
	public void testDoUnlockaNotLockedResource() throws Exception {

		ResourceLocks resLocks = new ResourceLocks();
		final String lockPath = "/notLockedResource";
		final String lockToken = "<opaquelocktoken:xxxx-xxxx-xxxxWRONG>";

		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(lockPath));

				oneOf(mockReq).getHeader(HttpHeaders.LOCK_TOKEN);
				will(returnValue(lockToken));

				oneOf(mockRes).sendError(HttpServletResponse.SC_BAD_REQUEST);
			}
		});

		DoUnlock doUnlock = new DoUnlock(mockStore, resLocks, !readOnly);

		doUnlock.execute(mockTransaction, mockReq, mockRes);

		_mockery.assertIsSatisfied();
	}

	@Test
	public void testDoUnlockaLockNullResource() throws Exception {

		final String parentPath = "/parentCollection";
		final String nullLoPath = parentPath.concat("/aNullResource");

		final PrintWriter pw = new PrintWriter(tmpFolder + "/XMLTestFile");

		final ByteArrayInputStream baisExclusive = new ByteArrayInputStream(exclusiveLockRequestByteArray);
		final DelegatingServletInputStream dsisExclusive = new DelegatingServletInputStream(baisExclusive);

		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(nullLoPath));

				LockedObject lockNullResourceLo = null;

				oneOf(mockResourceLocks).getLockedObjectByPath(mockTransaction, nullLoPath);
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

				oneOf(mockStore).getStoredObject(mockTransaction, nullLoPath);
				will(returnValue(lockNullResourceSo));

				StoredObject parentSo = null;

				oneOf(mockStore).getStoredObject(mockTransaction, parentPath);
				will(returnValue(parentSo));

				oneOf(mockStore).createFolder(mockTransaction, parentPath);

				oneOf(mockStore).getStoredObject(mockTransaction, nullLoPath);
				will(returnValue(lockNullResourceSo));

				oneOf(mockStore).createResource(mockTransaction, nullLoPath);

				oneOf(mockRes).setStatus(HttpServletResponse.SC_CREATED);

				lockNullResourceSo = initLockNullStoredObject();

				oneOf(mockStore).getStoredObject(mockTransaction, nullLoPath);
				will(returnValue(lockNullResourceSo));

				oneOf(mockReq).getInputStream();
				will(returnValue(dsisExclusive));

				oneOf(mockReq).getHeader(HttpHeaders.DEPTH);
				will(returnValue(("0")));

				oneOf(mockReq).getHeader(HttpHeaders.TIMEOUT);
				will(returnValue("Infinite"));

				ResourceLocks resLocks = ResourceLocks.class.newInstance();

				oneOf(mockResourceLocks).exclusiveLock(mockTransaction, nullLoPath, "I'am the Lock Owner", 0, AbstractMethod.getMaxTimeout());
				will(returnValue(true));

				lockNullResourceLo = initLockNullLockedObject(resLocks, nullLoPath);

				oneOf(mockResourceLocks).getLockedObjectByPath(mockTransaction, nullLoPath);
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

				// -----LOCK on a non-existing resource successful------
				// ----------------now try to unlock it-----------------

				oneOf(mockReq).getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(nullLoPath));

				oneOf(mockResourceLocks).lock(with(any(ITransaction.class)), with(any(String.class)),
						with(any(String.class)), with(any(boolean.class)), with(any(int.class)), with(any(int.class)),
						with(any(boolean.class)));
				will(returnValue(true));

				oneOf(mockReq).getHeader(HttpHeaders.LOCK_TOKEN);
				will(returnValue(lockToken));

				oneOf(mockResourceLocks).getLockedObjectByID(mockTransaction, loId);
				will(returnValue(lockNullResourceLo));

				String[] owners = (lockNullResourceLo != null ? lockNullResourceLo.getOwner() : null);
				String owner = null;
				if (owners != null) {
					owner = owners[0];
				}

				oneOf(mockResourceLocks).unlock(mockTransaction, loId, owner);
				will(returnValue(true));

				oneOf(mockStore).getStoredObject(mockTransaction, nullLoPath);
				will(returnValue(lockNullResourceSo));

				oneOf(mockStore).removeObject(mockTransaction, nullLoPath);

				oneOf(mockRes).setStatus(HttpServletResponse.SC_NO_CONTENT);

				oneOf(mockResourceLocks).unlockTemporaryLockedObjects(with(any(ITransaction.class)),
						with(any(String.class)), with(any(String.class)));

			}
		});

		DoLock doLock = new DoLock(mockStore, mockResourceLocks, !readOnly);
		doLock.execute(mockTransaction, mockReq, mockRes);

		DoUnlock doUnlock = new DoUnlock(mockStore, mockResourceLocks, !readOnly);
		doUnlock.execute(mockTransaction, mockReq, mockRes);

		_mockery.assertIsSatisfied();

	}

}
