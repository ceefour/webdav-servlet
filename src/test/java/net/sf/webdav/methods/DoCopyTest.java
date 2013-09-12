package net.sf.webdav.methods;

import java.io.ByteArrayInputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.webdav.ITransaction;
import net.sf.webdav.IWebdavStore;
import net.sf.webdav.StoredObject;
import net.sf.webdav.WebdavStatus;
import net.sf.webdav.locking.LockedObject;
import net.sf.webdav.locking.ResourceLocks;
import net.sf.webdav.testutil.MockTest;

import org.jmock.Expectations;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.DelegatingServletInputStream;

public class DoCopyTest extends MockTest {

    IWebdavStore mockStore;
    HttpServletRequest mockReq;
    HttpServletResponse mockRes;
    ITransaction mockTransaction;
    static ByteArrayInputStream bais = new ByteArrayInputStream(resourceContent);
    static DelegatingServletInputStream dsis = new DelegatingServletInputStream(
            bais);

    @Before
    public void setUp() throws Exception {
        mockStore = _mockery.mock(IWebdavStore.class);
        mockReq = _mockery.mock(HttpServletRequest.class);
        mockRes = _mockery.mock(HttpServletResponse.class);
        mockTransaction = _mockery.mock(ITransaction.class);
    }

    @Test
    public void testDoCopyIfReadOnly() throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                oneOf(mockRes).sendError(WebdavStatus.SC_FORBIDDEN);
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
        resLocks.lock(mockTransaction, parentPath, owner, true, 1,
                TEMP_TIMEOUT, !TEMPORARY);

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(path));

                oneOf(mockReq).getHeader("Destination");
                will(returnValue("/destination"));

                oneOf(mockReq).getServerName();
                will(returnValue("myServer"));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getPathInfo();
                will(returnValue("/destination"));

                oneOf(mockReq).getServletPath();
                will(returnValue("/servletPath"));

                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(path));

                oneOf(mockReq).getHeader("Overwrite");
                will(returnValue("T"));

                StoredObject so = initLockNullStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(so));

                oneOf(mockRes).addHeader("Allow",
                        "OPTIONS, MKCOL, PUT, PROPFIND, LOCK, UNLOCK");

                oneOf(mockRes).sendError(WebdavStatus.SC_METHOD_NOT_ALLOWED);
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
        resLocks.lock(mockTransaction, destCollectionPath, owner, true, 1,
                TEMP_TIMEOUT, !TEMPORARY);

        final LockedObject lo = resLocks.getLockedObjectByPath(mockTransaction,
                destCollectionPath);
        final String wrongLockToken = "(<opaquelocktoken:" + lo.getID()
                + "WRONG>)";

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                oneOf(mockReq).getHeader("Destination");
                will(returnValue(destFilePath));

                oneOf(mockReq).getServerName();
                will(returnValue("myServer"));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getPathInfo();
                will(returnValue(destFilePath));

                oneOf(mockReq).getServletPath();
                will(returnValue("/servletPath"));

                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                oneOf(mockReq).getHeader("If");
                will(returnValue(wrongLockToken));

                oneOf(mockRes).setStatus(WebdavStatus.SC_LOCKED);
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

        resLocks.lock(mockTransaction, destCollectionPath, owner, true, 1,
                TEMP_TIMEOUT, !TEMPORARY);

        final LockedObject lo = resLocks.getLockedObjectByPath(mockTransaction,
                destCollectionPath);
        final String rightLockToken = "(<opaquelocktoken:" + lo.getID() + ">)";

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                oneOf(mockReq).getHeader("Destination");
                will(returnValue(destFilePath));

                oneOf(mockReq).getServerName();
                will(returnValue("myServer"));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getPathInfo();
                will(returnValue(destFilePath));

                oneOf(mockReq).getServletPath();
                will(returnValue("/servletPath"));

                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                oneOf(mockReq).getHeader("If");
                will(returnValue(rightLockToken));

                oneOf(mockReq).getHeader("Overwrite");
                will(returnValue("F"));

                StoredObject sourceFileSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(sourceFileSo));

                StoredObject destFileSo = null;

                oneOf(mockStore).getStoredObject(mockTransaction, destFilePath);
                will(returnValue(destFileSo));

                oneOf(mockRes).setStatus(WebdavStatus.SC_CREATED);

                oneOf(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(sourceFileSo));

                oneOf(mockStore).createResource(mockTransaction, destFilePath);

                oneOf(mockStore).getResourceContent(mockTransaction,
                        sourceFilePath);
                will(returnValue(dsis));

                oneOf(mockStore).setResourceContent(mockTransaction,
                        destFilePath, dsis, null, null);
                will(returnValue(resourceLength));

                destFileSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, destFilePath);
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
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                oneOf(mockReq).getHeader("Destination");
                will(returnValue(null));

                oneOf(mockRes).sendError(WebdavStatus.SC_BAD_REQUEST);
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
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                oneOf(mockReq).getHeader("Destination");
                will(returnValue(sourceFilePath));

                oneOf(mockReq).getServerName();
                will(returnValue("serverName"));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                oneOf(mockReq).getServletPath();
                will(returnValue("/servletPath"));

                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                oneOf(mockRes).sendError(WebdavStatus.SC_FORBIDDEN);

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
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceCollectionPath));

                oneOf(mockReq).getHeader("Destination");
                will(returnValue(destCollectionPath));

                oneOf(mockReq).getServerName();
                will(returnValue("serverName"));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getPathInfo();
                will(returnValue(destCollectionPath));

                oneOf(mockReq).getServletPath();
                will(returnValue("/servletPath"));

                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceCollectionPath));

                oneOf(mockReq).getHeader("Overwrite");
                will(returnValue("F"));

                StoredObject sourceCollectionSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction,
                        sourceCollectionPath);
                will(returnValue(sourceCollectionSo));

                StoredObject destCollectionSo = null;

                oneOf(mockStore).getStoredObject(mockTransaction,
                        destCollectionPath);
                will(returnValue(destCollectionSo));

                oneOf(mockRes).setStatus(WebdavStatus.SC_CREATED);

                oneOf(mockStore).getStoredObject(mockTransaction,
                        sourceCollectionPath);
                will(returnValue(sourceCollectionSo));

                oneOf(mockStore)
                        .createFolder(mockTransaction, destCollectionPath);

                oneOf(mockReq).getHeader("Depth");
                will(returnValue("-1"));

                sourceChildren = new String[] { "sourceFile" };

                oneOf(mockStore).getChildrenNames(mockTransaction,
                        sourceCollectionPath);
                will(returnValue(sourceChildren));

                StoredObject sourceFileSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(sourceFileSo));

                oneOf(mockStore).createResource(mockTransaction,
                        destCollectionPath + "/sourceFile");

                oneOf(mockStore).getResourceContent(mockTransaction,
                        sourceFilePath);
                will(returnValue(dsis));

                oneOf(mockStore).setResourceContent(mockTransaction,
                        destCollectionPath + "/sourceFile", dsis, null, null);

                StoredObject destFileSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction,
                        destCollectionPath + "/sourceFile");
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
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                oneOf(mockReq).getHeader("Destination");
                will(returnValue(destFilePath));

                oneOf(mockReq).getServerName();
                will(returnValue("serverName"));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getPathInfo();
                will(returnValue(destFilePath));

                oneOf(mockReq).getServletPath();
                will(returnValue("/servletPath"));

                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                oneOf(mockReq).getHeader("Overwrite");
                will(returnValue("F"));

                StoredObject notExistSo = null;

                oneOf(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(notExistSo));

                oneOf(mockRes).sendError(WebdavStatus.SC_NOT_FOUND);

            }
        });

        ResourceLocks resLocks = new ResourceLocks();

        DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);

        DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !readOnly);
        doCopy.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDoCopyIfDestinationAlreadyExistsAndOverwriteTrue()
            throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                StoredObject sourceSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(sourceSo));

                oneOf(mockReq).getHeader("Destination");
                will(returnValue(destFilePath));

                oneOf(mockReq).getServerName();
                will(returnValue("serverName"));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getPathInfo();
                will(returnValue("/folder/destFolder"));

                oneOf(mockReq).getServletPath();
                will(returnValue("/servletPath"));

                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                StoredObject existingDestSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, destFilePath);
                will(returnValue(existingDestSo));

                oneOf(mockReq).getHeader("Overwrite");
                will(returnValue("T"));

                oneOf(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                oneOf(mockStore).getStoredObject(mockTransaction, destFilePath);
                will(returnValue(existingDestSo));

                oneOf(mockStore).removeObject(mockTransaction, destFilePath);

                oneOf(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(sourceSo));

                oneOf(mockStore).createResource(mockTransaction, destFilePath);

                oneOf(mockStore).getResourceContent(mockTransaction,
                        sourceFilePath);
                will(returnValue(dsis));

                oneOf(mockStore).setResourceContent(mockTransaction,
                        destFilePath, dsis, null, null);

                oneOf(mockStore).getStoredObject(mockTransaction, destFilePath);
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
    public void testDoCopyIfDestinationAlreadyExistsAndOverwriteFalse()
            throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                StoredObject sourceSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(sourceSo));

                oneOf(mockReq).getHeader("Destination");
                will(returnValue("serverName".concat(destFilePath)));

                oneOf(mockReq).getServerName();
                will(returnValue("serverName"));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getPathInfo();
                will(returnValue(destFilePath));

                oneOf(mockReq).getServletPath();
                will(returnValue("/servletPath"));

                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                StoredObject existingDestSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, destFilePath);
                will(returnValue(existingDestSo));

                oneOf(mockReq).getHeader("Overwrite");
                will(returnValue("F"));

                oneOf(mockRes).sendError(WebdavStatus.SC_PRECONDITION_FAILED);

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
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                StoredObject sourceSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(sourceSo));

                oneOf(mockReq).getHeader("Destination");
                will(returnValue("http://destination:80".concat(destFilePath)));

                oneOf(mockReq).getContextPath();
                will(returnValue("http://destination:80"));

                oneOf(mockReq).getPathInfo();
                will(returnValue(destCollectionPath));

                oneOf(mockReq).getServletPath();
                will(returnValue("http://destination:80"));

                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(sourceFilePath));

                oneOf(mockReq).getAttribute("javax.servlet.include.path_info");
                will(returnValue(sourceFilePath));

                oneOf(mockReq).getHeader("Overwrite");
                will(returnValue("T"));

                StoredObject destFileSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, destFilePath);
                will(returnValue(destFileSo));

                oneOf(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                oneOf(mockStore).getStoredObject(mockTransaction, destFilePath);
                will(returnValue(destFileSo));

                oneOf(mockStore).removeObject(mockTransaction, destFilePath);

                oneOf(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(sourceSo));

                oneOf(mockStore).createResource(mockTransaction, destFilePath);

                oneOf(mockStore).getResourceContent(mockTransaction,
                        sourceFilePath);
                will(returnValue(dsis));

                oneOf(mockStore).setResourceContent(mockTransaction,
                        destFilePath, dsis, null, null);

                oneOf(mockStore).getStoredObject(mockTransaction, destFilePath);
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
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                StoredObject sourceSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(sourceSo));

                oneOf(mockReq).getHeader("Destination");
                will(returnValue("http://destination:80"
                        .concat(destCollectionPath)));

                oneOf(mockReq).getContextPath();
                will(returnValue("http://destination:80"));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                oneOf(mockReq).getServletPath();
                will(returnValue("http://destination:80"));

                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(sourceFilePath));

                oneOf(mockReq).getAttribute("javax.servlet.include.path_info");
                will(returnValue(sourceFilePath));

                oneOf(mockReq).getHeader("Overwrite");
                will(returnValue("F"));

                StoredObject existingDestSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction,
                        destCollectionPath);
                will(returnValue(existingDestSo));

                oneOf(mockRes).sendError(WebdavStatus.SC_PRECONDITION_FAILED);
            }
        });

        ResourceLocks resLocks = new ResourceLocks();
        DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);

        DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !readOnly);
        doCopy.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();

    }
}
