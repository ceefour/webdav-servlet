package net.sf.webdav.methods;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.webdav.ITransaction;
import net.sf.webdav.IWebdavStore;
import net.sf.webdav.StoredObject;
import net.sf.webdav.WebdavStatus;
import net.sf.webdav.locking.IResourceLocks;
import net.sf.webdav.locking.LockedObject;
import net.sf.webdav.locking.ResourceLocks;
import net.sf.webdav.testutil.MockTest;

import org.jmock.Expectations;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.DelegatingServletInputStream;

public class DoUnlockTest extends MockTest {

    IWebdavStore mockStore;
    HttpServletRequest mockReq;
    HttpServletResponse mockRes;
    ITransaction mockTransaction;
    static IResourceLocks mockResourceLocks;

    static boolean exclusive = true;

    @Before
    public void setUp() throws Exception {
        mockStore = _mockery.mock(IWebdavStore.class);
        mockReq = _mockery.mock(HttpServletRequest.class);
        mockRes = _mockery.mock(HttpServletResponse.class);
        mockTransaction = _mockery.mock(ITransaction.class);
        mockResourceLocks = _mockery.mock(IResourceLocks.class);
    }

    @Test
    public void testDoUnlockIfReadOnly() throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockRes).sendError(WebdavStatus.SC_FORBIDDEN);
            }
        });

        DoUnlock doUnlock = new DoUnlock(mockStore, null,
                new ResourceLocks(), readOnly);

        doUnlock.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDoUnlockaLockedResourceWithRightLockToken()
            throws Exception {

        final String lockPath = "/lockedResource";
        final String lockOwner = "theOwner";

        ResourceLocks resLocks = new ResourceLocks();
        resLocks.lock(mockTransaction, lockPath, lockOwner, exclusive, 0,
                TEMP_TIMEOUT, !TEMPORARY);

        LockedObject lo = resLocks.getLockedObjectByPath(mockTransaction,
                lockPath);
        final String loID = lo.getID();
        final String lockToken = "<opaquelocktoken:".concat(loID).concat(">");

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(lockPath));

                oneOf(mockReq).getHeader("Lock-Token");
                will(returnValue(lockToken));

                StoredObject lockedSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, lockPath);
                will(returnValue(lockedSo));

                oneOf(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);
            }
        });

        DoUnlock doUnlock = new DoUnlock(mockStore, null, resLocks, !readOnly);

        doUnlock.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDoUnlockaLockedResourceWithWrongLockToken()
            throws Exception {

        final String lockPath = "/lockedResource";
        final String lockOwner = "theOwner";

        ResourceLocks resLocks = new ResourceLocks();
        resLocks.lock(mockTransaction, lockPath, lockOwner, exclusive, 0,
                TEMP_TIMEOUT, !TEMPORARY);

        LockedObject lo = resLocks.getLockedObjectByPath(mockTransaction,
                lockPath);
        final String loID = lo.getID();
        final String lockToken = "<opaquelocktoken:".concat(loID).concat(
                "WRONG>");

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(lockPath));

                oneOf(mockReq).getHeader("Lock-Token");
                will(returnValue(lockToken));

                oneOf(mockRes).sendError(WebdavStatus.SC_BAD_REQUEST);
            }
        });

        DoUnlock doUnlock = new DoUnlock(mockStore, null, resLocks, !readOnly);
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
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(lockPath));

                oneOf(mockReq).getHeader("Lock-Token");
                will(returnValue(lockToken));

                oneOf(mockRes).sendError(WebdavStatus.SC_BAD_REQUEST);
            }
        });

        DoUnlock doUnlock = new DoUnlock(mockStore, null, resLocks, !readOnly);

        doUnlock.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDoUnlockaLockNullResource() throws Exception {

        final String parentPath = "/parentCollection";
        final String nullLoPath = parentPath.concat("/aNullResource");

        final PrintWriter pw = new PrintWriter("/tmp/XMLTestFile");

        final ByteArrayInputStream baisExclusive = new ByteArrayInputStream(
                exclusiveLockRequestByteArray);
        final DelegatingServletInputStream dsisExclusive = new DelegatingServletInputStream(
                baisExclusive);

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(nullLoPath));

                LockedObject lockNullResourceLo = null;

                oneOf(mockResourceLocks).getLockedObjectByPath(mockTransaction,
                        nullLoPath);
                will(returnValue(lockNullResourceLo));

                LockedObject parentLo = null;

                oneOf(mockResourceLocks).getLockedObjectByPath(mockTransaction,
                        parentPath);
                will(returnValue(parentLo));

                oneOf(mockReq).getHeader("User-Agent");
                will(returnValue("Goliath"));

                oneOf(mockResourceLocks).lock(with(any(ITransaction.class)),
                        with(any(String.class)), with(any(String.class)),
                        with(any(boolean.class)), with(any(int.class)),
                        with(any(int.class)), with(any(boolean.class)));
                will(returnValue(true));

                oneOf(mockReq).getHeader("If");
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

                oneOf(mockRes).setStatus(WebdavStatus.SC_CREATED);

                lockNullResourceSo = initLockNullStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, nullLoPath);
                will(returnValue(lockNullResourceSo));

                oneOf(mockReq).getInputStream();
                will(returnValue(dsisExclusive));

                oneOf(mockReq).getHeader("Depth");
                will(returnValue(("0")));

                oneOf(mockReq).getHeader("Timeout");
                will(returnValue("Infinite"));

                ResourceLocks resLocks = ResourceLocks.class.newInstance();

                oneOf(mockResourceLocks).exclusiveLock(mockTransaction,
                        nullLoPath, "I'am the Lock Owner", 0, 604800);
                will(returnValue(true));

                lockNullResourceLo = initLockNullLockedObject(resLocks,
                        nullLoPath);

                oneOf(mockResourceLocks).getLockedObjectByPath(mockTransaction,
                        nullLoPath);
                will(returnValue(lockNullResourceLo));

                oneOf(mockRes).setStatus(WebdavStatus.SC_OK);

                oneOf(mockRes).setContentType("text/xml; charset=UTF-8");

                oneOf(mockRes).getWriter();
                will(returnValue(pw));

                String loId = null;
                if (lockNullResourceLo != null) {
                    loId = lockNullResourceLo.getID();
                }
                final String lockToken = "<opaquelocktoken:" + loId + ">";

                oneOf(mockRes).addHeader("Lock-Token", lockToken);

                oneOf(mockResourceLocks).unlockTemporaryLockedObjects(
                        with(any(ITransaction.class)), with(any(String.class)),
                        with(any(String.class)));

                // -----LOCK on a non-existing resource successful------
                // ----------------now try to unlock it-----------------

                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(nullLoPath));

                oneOf(mockResourceLocks).lock(with(any(ITransaction.class)),
                        with(any(String.class)), with(any(String.class)),
                        with(any(boolean.class)), with(any(int.class)),
                        with(any(int.class)), with(any(boolean.class)));
                will(returnValue(true));

                oneOf(mockReq).getHeader("Lock-Token");
                will(returnValue(lockToken));

                oneOf(mockResourceLocks).getLockedObjectByID(mockTransaction,
                        loId);
                will(returnValue(lockNullResourceLo));

                String[] owners = lockNullResourceLo.getOwner();
                String owner = null;
                if (owners != null)
                    owner = owners[0];

                oneOf(mockResourceLocks).unlock(mockTransaction, loId, owner);
                will(returnValue(true));

                oneOf(mockStore).getStoredObject(mockTransaction, nullLoPath);
                will(returnValue(lockNullResourceSo));

                oneOf(mockStore).removeObject(mockTransaction, nullLoPath);

                oneOf(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                oneOf(mockResourceLocks).unlockTemporaryLockedObjects(
                        with(any(ITransaction.class)), with(any(String.class)),
                        with(any(String.class)));

            }
        });

        DoLock doLock = new DoLock(mockStore, null, mockResourceLocks, !readOnly);
        doLock.execute(mockTransaction, mockReq, mockRes);

        DoUnlock doUnlock = new DoUnlock(mockStore, null,
                mockResourceLocks, !readOnly);
        doUnlock.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();

    }

}
