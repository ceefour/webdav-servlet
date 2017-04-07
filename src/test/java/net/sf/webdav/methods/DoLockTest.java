package net.sf.webdav.methods;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.webdav.ITransaction;
import net.sf.webdav.IWebDAVStore;
import net.sf.webdav.StoredObject;
import net.sf.webdav.WebDAVStatus;
import net.sf.webdav.locking.IResourceLocks;
import net.sf.webdav.locking.LockedObject;
import net.sf.webdav.locking.ResourceLocks;
import net.sf.webdav.testutil.MockTest;

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

        _mockery.checking(new Expectations() {
            {
                one(mockRes).sendError(WebDAVStatus.SC_FORBIDDEN);
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
        resLocks.lock(mockTransaction, lockPath, lockOwner, exclusive, depth,
                TEMP_TIMEOUT, !TEMPORARY);

        LockedObject lo = resLocks.getLockedObjectByPath(mockTransaction,
                lockPath);
        String lockTokenString = lo.getID();
        final String lockToken = "(<opaquelocktoken:" + lockTokenString + ">)";

        final PrintWriter pw = new PrintWriter("/tmp/XMLTestFile");

        _mockery.checking(new Expectations() {
            {
                one(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(lockPath));

                one(mockReq).getHeader("If");
                will(returnValue(lockToken));

                one(mockReq).getHeader("User-Agent");
                will(returnValue("Goliath"));

                exactly(2).of(mockReq).getHeader("If");
                will(returnValue(lockToken));

                one(mockReq).getHeader("Timeout");
                will(returnValue("Infinite"));

                one(mockRes).setStatus(WebDAVStatus.SC_OK);

                one(mockRes).setContentType("text/xml; charset=UTF-8");

                one(mockRes).getWriter();
                will(returnValue(pw));

                one(mockRes).addHeader(
                        "Lock-Token",
                        lockToken.substring(lockToken.indexOf("(") + 1,
                                lockToken.indexOf(")")));
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
        final PrintWriter pw = new PrintWriter("/tmp/XMLTestFile");

        final ByteArrayInputStream baisExclusive = new ByteArrayInputStream(
                exclusiveLockRequestByteArray);
        final DelegatingServletInputStream dsisExclusive = new DelegatingServletInputStream(
                baisExclusive);

        _mockery.checking(new Expectations() {
            {
                one(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(lockPath));

                one(mockReq).getHeader("User-Agent");
                will(returnValue("Goliath"));

                one(mockReq).getHeader("If");
                will(returnValue(null));

                StoredObject so = initFileStoredObject(resourceContent);

                one(mockStore).getStoredObject(mockTransaction, lockPath);
                will(returnValue(so));

                one(mockReq).getInputStream();
                will(returnValue(dsisExclusive));

                one(mockReq).getHeader("Depth");
                will(returnValue(depthString));

                one(mockReq).getHeader("Timeout");
                will(returnValue(timeoutString));

                one(mockRes).setStatus(WebDAVStatus.SC_OK);

                one(mockRes).setContentType("text/xml; charset=UTF-8");

                one(mockRes).getWriter();
                will(returnValue(pw));

                // addHeader("Lock-Token", "(<opaquelocktoken:xxx-xxx-xxx>)")
                one(mockRes).addHeader(with(any(String.class)),
                        with(any(String.class)));
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
        final PrintWriter pw = new PrintWriter("/tmp/XMLTestFile");

        final ByteArrayInputStream baisShared = new ByteArrayInputStream(
                sharedLockRequestByteArray);
        final DelegatingServletInputStream dsisShared = new DelegatingServletInputStream(
                baisShared);

        _mockery.checking(new Expectations() {
            {
                one(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(lockPath));

                one(mockReq).getHeader("User-Agent");
                will(returnValue("Goliath"));

                one(mockReq).getHeader("If");
                will(returnValue(null));

                StoredObject so = initFileStoredObject(resourceContent);

                one(mockStore).getStoredObject(mockTransaction, lockPath);
                will(returnValue(so));

                one(mockReq).getInputStream();
                will(returnValue(dsisShared));

                one(mockReq).getHeader("Depth");
                will(returnValue(depthString));

                one(mockReq).getHeader("Timeout");
                will(returnValue(timeoutString));

                one(mockRes).setStatus(WebDAVStatus.SC_OK);

                one(mockRes).setContentType("text/xml; charset=UTF-8");

                one(mockRes).getWriter();
                will(returnValue(pw));

                // addHeader("Lock-Token", "(<opaquelocktoken:xxx-xxx-xxx>)")
                one(mockRes).addHeader(with(any(String.class)),
                        with(any(String.class)));
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

        final PrintWriter pw = new PrintWriter("/tmp/XMLTestFile");

        final ByteArrayInputStream baisExclusive = new ByteArrayInputStream(
                exclusiveLockRequestByteArray);
        final DelegatingServletInputStream dsisExclusive = new DelegatingServletInputStream(
                baisExclusive);

        _mockery.checking(new Expectations() {
            {
                one(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(lockPath));

                one(mockReq).getHeader("User-Agent");
                will(returnValue("Goliath"));

                one(mockReq).getHeader("If");
                will(returnValue(null));

                StoredObject so = initFolderStoredObject();

                one(mockStore).getStoredObject(mockTransaction, lockPath);
                will(returnValue(so));

                one(mockReq).getInputStream();
                will(returnValue(dsisExclusive));

                one(mockReq).getHeader("Depth");
                will(returnValue(depthString));

                one(mockReq).getHeader("Timeout");
                will(returnValue(timeoutString));

                one(mockRes).setStatus(WebDAVStatus.SC_OK);

                one(mockRes).setContentType("text/xml; charset=UTF-8");

                one(mockRes).getWriter();
                will(returnValue(pw));

                // addHeader("Lock-Token", "(<opaquelocktoken:xxx-xxx-xxx>)")
                one(mockRes).addHeader(with(any(String.class)),
                        with(any(String.class)));
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
        final PrintWriter pw = new PrintWriter("/tmp/XMLTestFile");

        final ByteArrayInputStream baisShared = new ByteArrayInputStream(
                sharedLockRequestByteArray);
        final DelegatingServletInputStream dsisShared = new DelegatingServletInputStream(
                baisShared);

        _mockery.checking(new Expectations() {
            {
                one(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(lockPath));

                one(mockReq).getHeader("User-Agent");
                will(returnValue("Goliath"));

                one(mockReq).getHeader("If");
                will(returnValue(null));

                StoredObject so = initFolderStoredObject();

                one(mockStore).getStoredObject(mockTransaction, lockPath);
                will(returnValue(so));

                one(mockReq).getInputStream();
                will(returnValue(dsisShared));

                one(mockReq).getHeader("Depth");
                will(returnValue(depthString));

                one(mockReq).getHeader("Timeout");
                will(returnValue(timeoutString));

                one(mockRes).setStatus(WebDAVStatus.SC_OK);

                one(mockRes).setContentType("text/xml; charset=UTF-8");

                one(mockRes).getWriter();
                will(returnValue(pw));

                // addHeader("Lock-Token", "(<opaquelocktoken:xxx-xxx-xxx>)")
                one(mockRes).addHeader(with(any(String.class)),
                        with(any(String.class)));
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

        final PrintWriter pw = new PrintWriter("/tmp/XMLTestFile");

        final ByteArrayInputStream baisExclusive = new ByteArrayInputStream(
                exclusiveLockRequestByteArray);
        final DelegatingServletInputStream dsisExclusive = new DelegatingServletInputStream(
                baisExclusive);

        _mockery.checking(new Expectations() {
            {
                one(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(lockPath));

                LockedObject lockNullResourceLo = null;

                one(mockResourceLocks).getLockedObjectByPath(mockTransaction,
                        lockPath);
                will(returnValue(lockNullResourceLo));

                LockedObject parentLo = null;

                one(mockResourceLocks).getLockedObjectByPath(mockTransaction,
                        parentPath);
                will(returnValue(parentLo));

                one(mockReq).getHeader("User-Agent");
                will(returnValue("Goliath"));

                one(mockResourceLocks).lock(with(any(ITransaction.class)),
                        with(any(String.class)), with(any(String.class)),
                        with(any(boolean.class)), with(any(int.class)),
                        with(any(int.class)), with(any(boolean.class)));
                will(returnValue(true));

                one(mockReq).getHeader("If");
                will(returnValue(null));

                StoredObject lockNullResourceSo = null;

                one(mockStore).getStoredObject(mockTransaction, lockPath);
                will(returnValue(lockNullResourceSo));

                StoredObject parentSo = null;

                one(mockStore).getStoredObject(mockTransaction, parentPath);
                will(returnValue(parentSo));

                one(mockStore).createFolder(mockTransaction, parentPath);

                one(mockStore).getStoredObject(mockTransaction, lockPath);
                will(returnValue(lockNullResourceSo));

                one(mockStore).createResource(mockTransaction, lockPath);

                one(mockRes).setStatus(WebDAVStatus.SC_CREATED);

                lockNullResourceSo = initLockNullStoredObject();

                one(mockStore).getStoredObject(mockTransaction, lockPath);
                will(returnValue(lockNullResourceSo));

                one(mockReq).getInputStream();
                will(returnValue(dsisExclusive));

                one(mockReq).getHeader("Depth");
                will(returnValue(("0")));

                one(mockReq).getHeader("Timeout");
                will(returnValue("Infinite"));

                ResourceLocks resLocks = ResourceLocks.class.newInstance();

                one(mockResourceLocks).exclusiveLock(mockTransaction, lockPath,
                        "I'am the Lock Owner", 0, 604800);
                will(returnValue(true));

                lockNullResourceLo = initLockNullLockedObject(resLocks,
                        lockPath);

                one(mockResourceLocks).getLockedObjectByPath(mockTransaction,
                        lockPath);
                will(returnValue(lockNullResourceLo));

                one(mockRes).setStatus(WebDAVStatus.SC_OK);

                one(mockRes).setContentType("text/xml; charset=UTF-8");

                one(mockRes).getWriter();
                will(returnValue(pw));

                String loId = null;
                if (lockNullResourceLo != null) {
                    loId = lockNullResourceLo.getID();
                }
                final String lockToken = "<opaquelocktoken:" + loId + ">";

                one(mockRes).addHeader("Lock-Token", lockToken);

                one(mockResourceLocks).unlockTemporaryLockedObjects(
                        with(any(ITransaction.class)), with(any(String.class)),
                        with(any(String.class)));
            }
        });

        DoLock doLock = new DoLock(mockStore, mockResourceLocks, !readOnly);
        doLock.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();

    }
}
