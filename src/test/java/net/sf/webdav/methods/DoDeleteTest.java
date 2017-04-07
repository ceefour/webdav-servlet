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
    static byte[] resourceContent = new byte[] { '<', 'h', 'e', 'l', 'l', 'o',
            '/', '>' };

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
                one(mockRes).sendError(WebDAVStatus.SC_FORBIDDEN);
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
                one(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                one(mockRes).setStatus(WebDAVStatus.SC_NO_CONTENT);

                StoredObject fileSo = initFileStoredObject(resourceContent);

                one(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(fileSo));

                one(mockStore).removeObject(mockTransaction, sourceFilePath);
            }
        });

        DoDelete doDelete = new DoDelete(mockStore, new ResourceLocks(),
                !readOnly);

        doDelete.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDeleteFileIfObjectNotExists() throws Exception {

        _mockery.checking(new Expectations() {
            {
                one(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                one(mockRes).setStatus(WebDAVStatus.SC_NO_CONTENT);

                StoredObject fileSo = null;

                one(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(fileSo));

                one(mockRes).sendError(WebDAVStatus.SC_NOT_FOUND);
            }
        });

        DoDelete doDelete = new DoDelete(mockStore, new ResourceLocks(),
                !readOnly);

        doDelete.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDeleteFolderIfObjectExists() throws Exception {

        _mockery.checking(new Expectations() {
            {
                one(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(sourceCollectionPath));

                one(mockRes).setStatus(WebDAVStatus.SC_NO_CONTENT);

                StoredObject folderSo = initFolderStoredObject();

                one(mockStore).getStoredObject(mockTransaction,
                        sourceCollectionPath);
                will(returnValue(folderSo));

                one(mockStore).getChildrenNames(mockTransaction,
                        sourceCollectionPath);
                will(returnValue(new String[] { "subFolder", "sourceFile" }));

                StoredObject fileSo = initFileStoredObject(resourceContent);

                one(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(fileSo));

                one(mockStore).removeObject(mockTransaction, sourceFilePath);

                StoredObject subFolderSo = initFolderStoredObject();

                one(mockStore).getStoredObject(mockTransaction,
                        sourceCollectionPath + "/subFolder");
                will(returnValue(subFolderSo));

                one(mockStore).getChildrenNames(mockTransaction,
                        sourceCollectionPath + "/subFolder");
                will(returnValue(new String[] { "fileInSubFolder" }));

                StoredObject fileInSubFolderSo = initFileStoredObject(resourceContent);

                one(mockStore).getStoredObject(mockTransaction,
                        sourceCollectionPath + "/subFolder/fileInSubFolder");
                will(returnValue(fileInSubFolderSo));

                one(mockStore).removeObject(mockTransaction,
                        sourceCollectionPath + "/subFolder/fileInSubFolder");

                one(mockStore).removeObject(mockTransaction,
                        sourceCollectionPath + "/subFolder");

                one(mockStore).removeObject(mockTransaction,
                        sourceCollectionPath);
            }
        });

        DoDelete doDelete = new DoDelete(mockStore, new ResourceLocks(),
                !readOnly);

        doDelete.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDeleteFolderIfObjectNotExists() throws Exception {

        _mockery.checking(new Expectations() {
            {
                one(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(sourceCollectionPath));

                one(mockRes).setStatus(WebDAVStatus.SC_NO_CONTENT);

                StoredObject folderSo = null;

                one(mockStore).getStoredObject(mockTransaction,
                        sourceCollectionPath);
                will(returnValue(folderSo));

                one(mockRes).sendError(WebDAVStatus.SC_NOT_FOUND);
            }
        });

        DoDelete doDelete = new DoDelete(mockStore, new ResourceLocks(),
                !readOnly);

        doDelete.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDeleteFileInFolder() throws Exception {

        _mockery.checking(new Expectations() {
            {
                one(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                one(mockRes).setStatus(WebDAVStatus.SC_NO_CONTENT);

                StoredObject fileSo = initFileStoredObject(resourceContent);

                one(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(fileSo));

                one(mockStore).removeObject(mockTransaction, sourceFilePath);
            }
        });

        DoDelete doDelete = new DoDelete(mockStore, new ResourceLocks(),
                !readOnly);

        doDelete.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDeleteFileInLockedFolderWithWrongLockToken()
            throws Exception {

        final String lockedFolderPath = "/lockedFolder";
        final String fileInLockedFolderPath = lockedFolderPath
                .concat("/fileInLockedFolder");

        String owner = new String("owner");
        ResourceLocks resLocks = new ResourceLocks();

        resLocks.lock(mockTransaction, lockedFolderPath, owner, true, -1,
                TEMP_TIMEOUT, !TEMPORARY);
        LockedObject lo = resLocks.getLockedObjectByPath(mockTransaction,
                lockedFolderPath);
        final String wrongLockToken = "(<opaquelocktoken:" + lo.getID()
                + "WRONG>)";

        final PrintWriter pw = new PrintWriter("/tmp/XMLTestFile");

        _mockery.checking(new Expectations() {
            {
                one(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(fileInLockedFolderPath));

                one(mockReq).getHeader("If");
                will(returnValue(wrongLockToken));

                one(mockRes).setStatus(WebDAVStatus.SC_MULTI_STATUS);

                one(mockReq).getRequestURI();
                will(returnValue("http://foo.bar".concat(lockedFolderPath)));

                one(mockRes).getWriter();
                will(returnValue(pw));

            }
        });

        DoDelete doDelete = new DoDelete(mockStore, resLocks, !readOnly);

        doDelete.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDeleteFileInLockedFolderWithRightLockToken()
            throws Exception {

        final String path = "/lockedFolder/fileInLockedFolder";
        final String parentPath = "/lockedFolder";
        final String owner = new String("owner");
        ResourceLocks resLocks = new ResourceLocks();

        resLocks.lock(mockTransaction, parentPath, owner, true, -1,
                TEMP_TIMEOUT, !TEMPORARY);
        LockedObject lo = resLocks.getLockedObjectByPath(mockTransaction,
                "/lockedFolder");
        final String rightLockToken = "(<opaquelocktoken:" + lo.getID() + ">)";

        _mockery.checking(new Expectations() {
            {
                one(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(path));

                one(mockReq).getHeader("If");
                will(returnValue(rightLockToken));

                one(mockRes).setStatus(WebDAVStatus.SC_NO_CONTENT);

                StoredObject so = initFileStoredObject(resourceContent);

                one(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(so));

                one(mockStore).removeObject(mockTransaction, path);

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
                one(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue("/folder/file"));

                one(mockRes).setStatus(WebDAVStatus.SC_NO_CONTENT);

                StoredObject nonExistingSo = null;

                one(mockStore).getStoredObject(mockTransaction, "/folder/file");
                will(returnValue(nonExistingSo));

                one(mockRes).sendError(WebDAVStatus.SC_NOT_FOUND);
            }
        });

        DoDelete doDelete = new DoDelete(mockStore, new ResourceLocks(),
                readOnly);

        doDelete.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

}
