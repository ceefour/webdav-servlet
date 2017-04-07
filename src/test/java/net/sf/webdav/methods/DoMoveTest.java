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

import org.jmock.Expectations;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.DelegatingServletInputStream;

public class DoMoveTest extends MockTest {

    static IWebDAVStore mockStore;
    static HttpServletRequest mockReq;
    static HttpServletResponse mockRes;
    static ITransaction mockTransaction;
    static byte[] resourceContent = new byte[] { '<', 'h', 'e', 'l', 'l', 'o',
            '/', '>' };
    static ByteArrayInputStream bais = new ByteArrayInputStream(resourceContent);
    static DelegatingServletInputStream dsis = new DelegatingServletInputStream(
            bais);

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
                one(mockRes).sendError(WebDAVStatus.SC_FORBIDDEN);
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
                one(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                exactly(2).of(mockReq).getHeader("Destination");
                will(returnValue(destFilePath));

                one(mockReq).getServerName();
                will(returnValue("serverName"));

                one(mockReq).getContextPath();
                will(returnValue(""));

                one(mockReq).getPathInfo();
                will(returnValue(destFilePath));

                one(mockReq).getServletPath();
                will(returnValue("/servletPath"));

                one(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                one(mockReq).getHeader("Overwrite");
                will(returnValue("F"));

                StoredObject sourceFileSo = initFileStoredObject(resourceContent);

                one(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(sourceFileSo));

                StoredObject destFileSo = null;

                one(mockStore).getStoredObject(mockTransaction, destFilePath);
                will(returnValue(destFileSo));

                one(mockRes).setStatus(WebDAVStatus.SC_CREATED);

                one(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(sourceFileSo));

                one(mockStore).createResource(mockTransaction, destFilePath);

                one(mockStore).getResourceContent(mockTransaction,
                        sourceFilePath);
                will(returnValue(dsis));

                one(mockStore).setResourceContent(mockTransaction,
                        destFilePath, dsis, null, null);
                will(returnValue(8L));

                destFileSo = initFileStoredObject(resourceContent);

                one(mockStore).getStoredObject(mockTransaction, destFilePath);
                will(returnValue(destFileSo));

                one(mockRes).setStatus(WebDAVStatus.SC_NO_CONTENT);

                one(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(sourceFileSo));

                one(mockStore).removeObject(mockTransaction, sourceFilePath);
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
    public void testMovingOfaFileIfDestinationIsPresentAndOverwriteFalse()
            throws Exception {

        _mockery.checking(new Expectations() {
            {
                one(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                exactly(2).of(mockReq).getHeader("Destination");
                will(returnValue(destFilePath));

                one(mockReq).getServerName();
                will(returnValue("server_name"));

                one(mockReq).getContextPath();
                will(returnValue(""));

                one(mockReq).getPathInfo();
                will(returnValue(destFilePath));

                one(mockReq).getServletPath();
                will(returnValue("servlet_path"));

                one(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                one(mockReq).getHeader("Overwrite");
                will(returnValue("F"));

                StoredObject sourceFileSo = initFileStoredObject(resourceContent);

                one(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(sourceFileSo));

                StoredObject destFileSo = initFileStoredObject(resourceContent);

                one(mockStore).getStoredObject(mockTransaction, destFilePath);
                will(returnValue(destFileSo));

                one(mockRes).sendError(WebDAVStatus.SC_PRECONDITION_FAILED);

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
    public void testMovingOfaFileIfDestinationIsPresentAndOverwriteTrue()
            throws Exception {

        _mockery.checking(new Expectations() {
            {
                one(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                exactly(2).of(mockReq).getHeader("Destination");
                will(returnValue(destFilePath));

                one(mockReq).getServerName();
                will(returnValue("server_name"));

                one(mockReq).getContextPath();
                will(returnValue(""));

                one(mockReq).getPathInfo();
                will(returnValue(destFilePath));

                one(mockReq).getServletPath();
                will(returnValue("servlet_path"));

                one(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                one(mockReq).getHeader("Overwrite");
                will(returnValue("T"));

                StoredObject sourceFileSo = initFileStoredObject(resourceContent);

                one(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(sourceFileSo));

                StoredObject destFileSo = initFileStoredObject(resourceContent);

                one(mockStore).getStoredObject(mockTransaction, destFilePath);
                will(returnValue(destFileSo));

                one(mockRes).setStatus(WebDAVStatus.SC_NO_CONTENT);

                one(mockStore).getStoredObject(mockTransaction, destFilePath);
                will(returnValue(destFileSo));

                one(mockStore).removeObject(mockTransaction, destFilePath);

                one(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(sourceFileSo));

                one(mockStore).createResource(mockTransaction, destFilePath);

                one(mockStore).getResourceContent(mockTransaction,
                        sourceFilePath);
                will(returnValue(dsis));

                one(mockStore).setResourceContent(mockTransaction,
                        destFilePath, dsis, null, null);
                will(returnValue(8L));

                one(mockStore).getStoredObject(mockTransaction, destFilePath);
                will(returnValue(destFileSo));

                one(mockRes).setStatus(WebDAVStatus.SC_NO_CONTENT);

                one(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(sourceFileSo));

                one(mockStore).removeObject(mockTransaction, sourceFilePath);
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
                one(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                exactly(2).of(mockReq).getHeader("Destination");
                will(returnValue(destFilePath));

                one(mockReq).getServerName();
                will(returnValue("server_name"));

                one(mockReq).getContextPath();
                will(returnValue(""));

                one(mockReq).getPathInfo();
                will(returnValue(destFilePath));

                one(mockReq).getServletPath();
                will(returnValue("servlet_path"));

                one(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                one(mockReq).getHeader("Overwrite");
                will(returnValue("F"));

                StoredObject sourceFileSo = null;

                one(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(sourceFileSo));

                one(mockRes).sendError(WebDAVStatus.SC_NOT_FOUND);
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
                one(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(destFilePath));

                exactly(2).of(mockReq).getHeader("Destination");
                will(returnValue(destFilePath));

                one(mockReq).getServerName();
                will(returnValue("server_name"));

                one(mockReq).getContextPath();
                will(returnValue(""));

                one(mockReq).getPathInfo();
                will(returnValue(destFilePath));

                one(mockReq).getServletPath();
                will(returnValue("servlet_path"));

                one(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(destFilePath));

                one(mockRes).sendError(WebDAVStatus.SC_FORBIDDEN);
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
    public void testMovingOfaCollectionIfDestinationIsNotPresent()
            throws Exception {

        _mockery.checking(new Expectations() {
            {
                one(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(sourceCollectionPath));

                exactly(2).of(mockReq).getHeader("Destination");
                will(returnValue(destCollectionPath));

                one(mockReq).getServerName();
                will(returnValue("server_name"));

                one(mockReq).getContextPath();
                will(returnValue(""));

                one(mockReq).getPathInfo();
                will(returnValue(destCollectionPath));

                one(mockReq).getServletPath();
                will(returnValue("servlet_path"));

                one(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(sourceCollectionPath));

                one(mockReq).getHeader("Overwrite");
                will(returnValue("F"));

                StoredObject sourceCollectionSo = initFolderStoredObject();

                one(mockStore).getStoredObject(mockTransaction,
                        sourceCollectionPath);
                will(returnValue(sourceCollectionSo));

                StoredObject destCollectionSo = null;

                one(mockStore).getStoredObject(mockTransaction,
                        destCollectionPath);
                will(returnValue(destCollectionSo));

                one(mockRes).setStatus(WebDAVStatus.SC_CREATED);

                one(mockStore).getStoredObject(mockTransaction,
                        sourceCollectionPath);
                will(returnValue(sourceCollectionSo));

                one(mockStore)
                        .createFolder(mockTransaction, destCollectionPath);

                one(mockReq).getHeader("Depth");
                will(returnValue(null));

                String[] sourceChildren = new String[] { "sourceFile" };

                one(mockStore).getChildrenNames(mockTransaction,
                        sourceCollectionPath);
                will(returnValue(sourceChildren));

                StoredObject sourceFileSo = initFileStoredObject(resourceContent);

                one(mockStore).getStoredObject(mockTransaction,
                        sourceCollectionPath + "/sourceFile");
                will(returnValue(sourceFileSo));

                one(mockStore).createResource(mockTransaction,
                        destCollectionPath + "/sourceFile");

                one(mockStore).getResourceContent(mockTransaction,
                        sourceCollectionPath + "/sourceFile");
                will(returnValue(dsis));

                one(mockStore).setResourceContent(mockTransaction,
                        destCollectionPath + "/sourceFile", dsis, null, null);
                will(returnValue(8L));

                StoredObject movedSo = initFileStoredObject(resourceContent);

                one(mockStore).getStoredObject(mockTransaction,
                        destCollectionPath + "/sourceFile");
                will(returnValue(movedSo));

                one(mockRes).setStatus(WebDAVStatus.SC_NO_CONTENT);

                one(mockStore).getStoredObject(mockTransaction,
                        sourceCollectionPath);
                will(returnValue(sourceCollectionSo));

                sourceChildren = new String[] { "sourceFile" };

                one(mockStore).getChildrenNames(mockTransaction,
                        sourceCollectionPath);
                will(returnValue(sourceChildren));

                one(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(sourceFileSo));

                one(mockStore).removeObject(mockTransaction, sourceFilePath);

                one(mockStore).removeObject(mockTransaction,
                        sourceCollectionPath);
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
    public void testMovingOfaCollectionIfDestinationIsPresentAndOverwriteFalse()
            throws Exception {

        _mockery.checking(new Expectations() {
            {
                one(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(sourceCollectionPath));

                exactly(2).of(mockReq).getHeader("Destination");
                will(returnValue(destCollectionPath));

                one(mockReq).getServerName();
                will(returnValue("server_name"));

                one(mockReq).getContextPath();
                will(returnValue(""));

                one(mockReq).getPathInfo();
                will(returnValue(destCollectionPath));

                one(mockReq).getServletPath();
                will(returnValue("servlet_path"));

                one(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(sourceCollectionPath));

                one(mockReq).getHeader("Overwrite");
                will(returnValue("F"));

                StoredObject sourceCollectionSo = initFolderStoredObject();

                one(mockStore).getStoredObject(mockTransaction,
                        sourceCollectionPath);
                will(returnValue(sourceCollectionSo));

                StoredObject destCollectionSo = initFolderStoredObject();

                one(mockStore).getStoredObject(mockTransaction,
                        destCollectionPath);
                will(returnValue(destCollectionSo));

                one(mockRes).sendError(WebDAVStatus.SC_PRECONDITION_FAILED);
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
    public void testMovingOfaCollectionIfDestinationIsPresentAndOverwriteTrue()
            throws Exception {

        _mockery.checking(new Expectations() {
            {
                one(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(sourceCollectionPath));

                exactly(2).of(mockReq).getHeader("Destination");
                will(returnValue(overwritePath));

                one(mockReq).getServerName();
                will(returnValue("server_name"));

                one(mockReq).getContextPath();
                will(returnValue(""));

                one(mockReq).getPathInfo();
                will(returnValue(overwritePath));

                one(mockReq).getServletPath();
                will(returnValue("servlet_path"));

                one(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(sourceCollectionPath));

                one(mockReq).getHeader("Overwrite");
                will(returnValue("T"));

                StoredObject sourceCollectionSo = initFolderStoredObject();

                one(mockStore).getStoredObject(mockTransaction,
                        sourceCollectionPath);
                will(returnValue(sourceCollectionSo));

                StoredObject destCollectionSo = initFolderStoredObject();

                one(mockStore).getStoredObject(mockTransaction, overwritePath);
                will(returnValue(destCollectionSo));

                one(mockRes).setStatus(WebDAVStatus.SC_NO_CONTENT);

                one(mockStore).getStoredObject(mockTransaction, overwritePath);
                will(returnValue(destCollectionSo));

                one(mockStore).getChildrenNames(mockTransaction, overwritePath);
                will(returnValue(destChildren));

                StoredObject destFileSo = initFileStoredObject(resourceContent);

                one(mockStore).getStoredObject(mockTransaction,
                        overwritePath + "/destFile");
                will(returnValue(destFileSo));

                one(mockStore).removeObject(mockTransaction,
                        overwritePath + "/destFile");

                one(mockStore).removeObject(mockTransaction, overwritePath);

                one(mockStore).getStoredObject(mockTransaction,
                        sourceCollectionPath);
                will(returnValue(sourceCollectionSo));

                one(mockStore).createFolder(mockTransaction, overwritePath);

                one(mockReq).getHeader("Depth");
                will(returnValue(null));

                one(mockStore).getChildrenNames(mockTransaction,
                        sourceCollectionPath);
                will(returnValue(sourceChildren));

                StoredObject sourceFileSo = initFileStoredObject(resourceContent);

                one(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(sourceFileSo));

                one(mockStore).createResource(mockTransaction,
                        overwritePath + "/sourceFile");

                one(mockStore).getResourceContent(mockTransaction,
                        sourceFilePath);
                will(returnValue(dsis));

                one(mockStore).setResourceContent(mockTransaction,
                        overwritePath + "/sourceFile", dsis, null, null);

                StoredObject movedSo = initFileStoredObject(resourceContent);

                one(mockStore).getStoredObject(mockTransaction,
                        overwritePath + "/sourceFile");
                will(returnValue(movedSo));

                one(mockRes).setStatus(WebDAVStatus.SC_NO_CONTENT);

                one(mockStore).getStoredObject(mockTransaction,
                        sourceCollectionPath);
                will(returnValue(sourceCollectionSo));

                sourceChildren = new String[] { "sourceFile" };

                one(mockStore).getChildrenNames(mockTransaction,
                        sourceCollectionPath);
                will(returnValue(sourceChildren));

                one(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(sourceFileSo));

                one(mockStore).removeObject(mockTransaction, sourceFilePath);

                one(mockStore).removeObject(mockTransaction,
                        sourceCollectionPath);
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
