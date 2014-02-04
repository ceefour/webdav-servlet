package net.sf.webdav.methods;

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
import org.junit.BeforeClass;
import org.junit.Test;

public class DoPutTest extends MockTest {
    static IWebdavStore mockStore;
    static HttpServletRequest mockReq;
    static HttpServletResponse mockRes;
    static IResourceLocks mockResourceLocks;
    static ITransaction mockTransaction;

    static String parentPath = "/parentCollection";
    static String path = parentPath.concat("/fileToPut");

    static boolean lazyFolderCreationOnPut = true;

    @BeforeClass
    public static void setUp() throws Exception {
        mockStore = _mockery.mock(IWebdavStore.class);
        mockReq = _mockery.mock(HttpServletRequest.class);
        mockRes = _mockery.mock(HttpServletResponse.class);
        mockResourceLocks = _mockery.mock(IResourceLocks.class);
        mockTransaction = _mockery.mock(ITransaction.class);
    }

    @Test
    public void testDoPutIfReadOnlyTrue() throws Exception {

        _mockery.checking(new Expectations() {
            {
                one(mockRes).sendError(WebdavStatus.SC_FORBIDDEN);
            }
        });

        DoPut doPut = new DoPut(mockStore, new ResourceLocks(), readOnly,
                lazyFolderCreationOnPut);
        doPut.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDoPutIfReadOnlyFalse() throws Exception {

        _mockery.checking(new Expectations() {
            {
                one(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(path));

                one(mockReq).getHeader("User-Agent");
                will(returnValue("Goliath agent"));

                StoredObject parentSo = initFolderStoredObject();

                one(mockStore).getStoredObject(mockTransaction, parentPath);
                will(returnValue(parentSo));

                StoredObject fileSo = null;

                one(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(fileSo));

                one(mockStore).createResource(mockTransaction, path);

                one(mockRes).setStatus(WebdavStatus.SC_CREATED);

                one(mockReq).getInputStream();
                will(returnValue(dsis));

                one(mockStore).setResourceContent(mockTransaction, path, dsis,
                        null, null);
                will(returnValue(8L));

                fileSo = initFileStoredObject(resourceContent);

                one(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(fileSo));

                // User-Agent: Goliath --> dont add ContentLength
                // one(mockRes).setContentLength(8);
            }
        });

        DoPut doPut = new DoPut(mockStore, new ResourceLocks(), !readOnly,
                lazyFolderCreationOnPut);
        doPut.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDoPutIfLazyFolderCreationOnPutIsFalse() throws Exception {

        _mockery.checking(new Expectations() {
            {
                one(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(path));

                one(mockReq).getHeader("User-Agent");
                will(returnValue("Transmit agent"));

                StoredObject parentSo = null;

                one(mockStore).getStoredObject(mockTransaction, parentPath);
                will(returnValue(parentSo));

                one(mockRes).sendError(
                    WebdavStatus.SC_NOT_FOUND, WebdavStatus.getStatusText(WebdavStatus.SC_NOT_FOUND)
                );

            }
        });

        DoPut doPut = new DoPut(mockStore, new ResourceLocks(), !readOnly,
                !lazyFolderCreationOnPut);
        doPut.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDoPutIfLazyFolderCreationOnPutIsTrue() throws Exception {

        _mockery.checking(new Expectations() {
            {
                one(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(path));

                one(mockReq).getHeader("User-Agent");
                will(returnValue("WebDAVFS/1.5.0 (01500000) ....."));

                StoredObject parentSo = null;

                one(mockStore).getStoredObject(mockTransaction, parentPath);
                will(returnValue(parentSo));

                one(mockStore).createFolder(mockTransaction, parentPath);

                StoredObject fileSo = null;

                one(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(fileSo));

                one(mockStore).createResource(mockTransaction, path);

                one(mockRes).setStatus(WebdavStatus.SC_CREATED);

                one(mockReq).getInputStream();
                will(returnValue(dsis));

                one(mockStore).setResourceContent(mockTransaction, path, dsis,
                        null, null);
                will(returnValue(8L));

                fileSo = initFileStoredObject(resourceContent);

                one(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(fileSo));

            }
        });

        DoPut doPut = new DoPut(mockStore, new ResourceLocks(), !readOnly,
                lazyFolderCreationOnPut);
        doPut.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDoPutIfParentPathIsResource() throws Exception {

        _mockery.checking(new Expectations() {
            {
                one(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(path));

                one(mockReq).getHeader("User-Agent");
                will(returnValue("WebDAVFS/1.5.0 (01500000) ....."));

                StoredObject parentSo = initFileStoredObject(resourceContent);

                one(mockStore).getStoredObject(mockTransaction, parentPath);
                will(returnValue(parentSo));

                one(mockRes).sendError(WebdavStatus.SC_FORBIDDEN);
            }
        });

        DoPut doPut = new DoPut(mockStore, new ResourceLocks(), !readOnly,
                lazyFolderCreationOnPut);
        doPut.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDoPutOnALockNullResource() throws Exception {

        final PrintWriter pw = new PrintWriter("/tmp/XMLTestFile");

        _mockery.checking(new Expectations() {
            {
                one(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(path));

                LockedObject lockNullResourceLo = null;

                one(mockResourceLocks).getLockedObjectByPath(mockTransaction,
                        path);
                will(returnValue(lockNullResourceLo));

                LockedObject parentLo = null;

                one(mockResourceLocks).getLockedObjectByPath(mockTransaction,
                        parentPath);
                will(returnValue(parentLo));

                one(mockReq).getHeader("User-Agent");
                will(returnValue("Transmit agent"));

                one(mockResourceLocks).lock(with(any(ITransaction.class)),
                        with(any(String.class)), with(any(String.class)),
                        with(any(boolean.class)), with(any(int.class)),
                        with(any(int.class)), with(any(boolean.class)));
                will(returnValue(true));

                one(mockReq).getHeader("If");
                will(returnValue(null));

                StoredObject lockNullResourceSo = null;

                one(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(lockNullResourceSo));

                StoredObject parentSo = null;

                one(mockStore).getStoredObject(mockTransaction, parentPath);
                will(returnValue(parentSo));

                one(mockStore).createFolder(mockTransaction, parentPath);

                parentSo = initFolderStoredObject();

                one(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(lockNullResourceSo));

                one(mockStore).createResource(mockTransaction, path);

                lockNullResourceSo = initLockNullStoredObject();

                one(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                one(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(lockNullResourceSo));

                one(mockReq).getInputStream();
                will(returnValue(dsisExclusive));

                one(mockReq).getHeader("Depth");
                will(returnValue(("0")));

                one(mockReq).getHeader("Timeout");
                will(returnValue("Infinite"));

                ResourceLocks resLocks = ResourceLocks.class.newInstance();

                one(mockResourceLocks).exclusiveLock(mockTransaction, path,
                        "I'am the Lock Owner", 0, 604800);
                will(returnValue(true));

                lockNullResourceLo = initLockNullLockedObject(resLocks, path);

                one(mockResourceLocks).getLockedObjectByPath(mockTransaction,
                        path);
                will(returnValue(lockNullResourceLo));

                one(mockRes).setStatus(WebdavStatus.SC_OK);

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

                // // -----LOCK on a non-existing resource successful------
                // // --------now doPUT on the lock-null resource----------

                one(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(path));

                one(mockReq).getHeader("User-Agent");
                will(returnValue("Transmit agent"));

                one(mockResourceLocks).getLockedObjectByPath(mockTransaction,
                        parentPath);
                will(returnValue(parentLo));

                one(mockResourceLocks).getLockedObjectByPath(mockTransaction,
                        path);
                will(returnValue(lockNullResourceLo));

                final String ifHeaderLockToken = "(<locktoken:" + loId + ">)";

                one(mockReq).getHeader("If");
                will(returnValue(ifHeaderLockToken));

                one(mockResourceLocks).getLockedObjectByID(mockTransaction,
                        loId);
                will(returnValue(lockNullResourceLo));

                one(mockResourceLocks).lock(with(any(ITransaction.class)),
                        with(any(String.class)), with(any(String.class)),
                        with(any(boolean.class)), with(any(int.class)),
                        with(any(int.class)), with(any(boolean.class)));
                will(returnValue(true));

                parentSo = initFolderStoredObject();

                one(mockStore).getStoredObject(mockTransaction, parentPath);
                will(returnValue(parentSo));

                one(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(lockNullResourceSo));

                one(mockResourceLocks).getLockedObjectByPath(mockTransaction,
                        path);
                will(returnValue(lockNullResourceLo));

                one(mockReq).getHeader("If");
                will(returnValue(ifHeaderLockToken));

                String[] owners = lockNullResourceLo.getOwner();
                String owner = null;
                if (owners != null) {
                    owner = owners[0];
                }

                one(mockResourceLocks).unlock(mockTransaction, loId, owner);
                will(returnValue(true));

                one(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                one(mockReq).getInputStream();
                will(returnValue(dsis));

                one(mockStore).setResourceContent(mockTransaction, path, dsis,
                        null, null);
                will(returnValue(8L));

                StoredObject newResourceSo = initFileStoredObject(resourceContent);

                one(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(newResourceSo));

                one(mockResourceLocks).unlockTemporaryLockedObjects(
                        with(any(ITransaction.class)), with(any(String.class)),
                        with(any(String.class)));
            }
        });

        DoLock doLock = new DoLock(mockStore, mockResourceLocks, !readOnly);
        doLock.execute(mockTransaction, mockReq, mockRes);

        DoPut doPut = new DoPut(mockStore, mockResourceLocks, !readOnly,
                lazyFolderCreationOnPut);
        doPut.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }
}
