package net.sf.webdav.methods;

import java.io.ByteArrayInputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.webdav.ITransaction;
import net.sf.webdav.IWebdavStore;
import net.sf.webdav.StoredObject;
import net.sf.webdav.WebdavStatus;
import net.sf.webdav.locking.ResourceLocks;
import net.sf.webdav.testutil.MockTest;

import org.jmock.Expectations;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.mock.web.DelegatingServletInputStream;

public class DoMoveTest extends MockTest {

    IWebdavStore mockStore;
    HttpServletRequest mockReq;
    HttpServletResponse mockRes;
    ITransaction mockTransaction;
    byte[] resourceContent = new byte[] { '<', 'h', 'e', 'l', 'l', 'o',
            '/', '>' };
    ByteArrayInputStream bais = new ByteArrayInputStream(resourceContent);
    DelegatingServletInputStream dsis = new DelegatingServletInputStream(
            bais);

    static final String tmpFolder = "/tmp/tests";

    static final String sourceCollectionPath = tmpFolder + "/sourceFolder";
    static final String destCollectionPath = tmpFolder + "/destFolder";
    static final String sourceFilePath = sourceCollectionPath + "/sourceFile";
    static final String destFilePath = destCollectionPath + "/destFile";

    static final String overwritePath = destCollectionPath + "/sourceFolder";

    @Before
    public void setUp() throws Exception {
        mockStore = _mockery.mock(IWebdavStore.class);
        mockReq = _mockery.mock(HttpServletRequest.class);
        mockRes = _mockery.mock(HttpServletResponse.class);
        mockTransaction = _mockery.mock(ITransaction.class);

    }

    @Test
    public void testMovingOfFileOrFolderIfReadOnlyIsTrue() throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockRes).sendError(WebdavStatus.SC_FORBIDDEN);
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
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                exactly(2).of(mockReq).getHeader("Destination");
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
                will(returnValue(8L));

                destFileSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, destFilePath);
                will(returnValue(destFileSo));

                oneOf(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                oneOf(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(sourceFileSo));

                oneOf(mockStore).removeObject(mockTransaction, sourceFilePath);
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
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                exactly(2).of(mockReq).getHeader("Destination");
                will(returnValue(destFilePath));

                oneOf(mockReq).getServerName();
                will(returnValue("server_name"));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getPathInfo();
                will(returnValue(destFilePath));

                oneOf(mockReq).getServletPath();
                will(returnValue("servlet_path"));

                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                oneOf(mockReq).getHeader("Overwrite");
                will(returnValue("F"));

                StoredObject sourceFileSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(sourceFileSo));

                StoredObject destFileSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, destFilePath);
                will(returnValue(destFileSo));

                oneOf(mockRes).sendError(WebdavStatus.SC_PRECONDITION_FAILED);

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
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                exactly(2).of(mockReq).getHeader("Destination");
                will(returnValue(destFilePath));

                oneOf(mockReq).getServerName();
                will(returnValue("server_name"));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getPathInfo();
                will(returnValue(destFilePath));

                oneOf(mockReq).getServletPath();
                will(returnValue("servlet_path"));

                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                oneOf(mockReq).getHeader("Overwrite");
                will(returnValue("T"));

                StoredObject sourceFileSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(sourceFileSo));

                StoredObject destFileSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, destFilePath);
                will(returnValue(destFileSo));

                oneOf(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                oneOf(mockStore).getStoredObject(mockTransaction, destFilePath);
                will(returnValue(destFileSo));

                oneOf(mockStore).removeObject(mockTransaction, destFilePath);

                oneOf(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(sourceFileSo));

                oneOf(mockStore).createResource(mockTransaction, destFilePath);

                oneOf(mockStore).getResourceContent(mockTransaction,
                        sourceFilePath);
                will(returnValue(dsis));

                oneOf(mockStore).setResourceContent(mockTransaction,
                        destFilePath, dsis, null, null);
                will(returnValue(8L));

                oneOf(mockStore).getStoredObject(mockTransaction, destFilePath);
                will(returnValue(destFileSo));

                oneOf(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                oneOf(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(sourceFileSo));

                oneOf(mockStore).removeObject(mockTransaction, sourceFilePath);
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
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                exactly(2).of(mockReq).getHeader("Destination");
                will(returnValue(destFilePath));

                oneOf(mockReq).getServerName();
                will(returnValue("server_name"));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getPathInfo();
                will(returnValue(destFilePath));

                oneOf(mockReq).getServletPath();
                will(returnValue("servlet_path"));

                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceFilePath));

                oneOf(mockReq).getHeader("Overwrite");
                will(returnValue("F"));

                StoredObject sourceFileSo = null;

                oneOf(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(sourceFileSo));

                oneOf(mockRes).sendError(WebdavStatus.SC_NOT_FOUND);
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
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(destFilePath));

                exactly(2).of(mockReq).getHeader("Destination");
                will(returnValue(destFilePath));

                oneOf(mockReq).getServerName();
                will(returnValue("server_name"));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getPathInfo();
                will(returnValue(destFilePath));

                oneOf(mockReq).getServletPath();
                will(returnValue("servlet_path"));

                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(destFilePath));

                oneOf(mockRes).sendError(WebdavStatus.SC_FORBIDDEN);
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
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceCollectionPath));

                exactly(2).of(mockReq).getHeader("Destination");
                will(returnValue(destCollectionPath));

                oneOf(mockReq).getServerName();
                will(returnValue("server_name"));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getPathInfo();
                will(returnValue(destCollectionPath));

                oneOf(mockReq).getServletPath();
                will(returnValue("servlet_path"));

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
                will(returnValue(null));

                String[] sourceChildren = new String[] { "sourceFile" };

                oneOf(mockStore).getChildrenNames(mockTransaction,
                        sourceCollectionPath);
                will(returnValue(sourceChildren));

                StoredObject sourceFileSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction,
                        sourceCollectionPath + "/sourceFile");
                will(returnValue(sourceFileSo));

                oneOf(mockStore).createResource(mockTransaction,
                        destCollectionPath + "/sourceFile");

                oneOf(mockStore).getResourceContent(mockTransaction,
                        sourceCollectionPath + "/sourceFile");
                will(returnValue(dsis));

                oneOf(mockStore).setResourceContent(mockTransaction,
                        destCollectionPath + "/sourceFile", dsis, null, null);
                will(returnValue(8L));

                StoredObject movedSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction,
                        destCollectionPath + "/sourceFile");
                will(returnValue(movedSo));

                oneOf(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                oneOf(mockStore).getStoredObject(mockTransaction,
                        sourceCollectionPath);
                will(returnValue(sourceCollectionSo));

                sourceChildren = new String[] { "sourceFile" };

                oneOf(mockStore).getChildrenNames(mockTransaction,
                        sourceCollectionPath);
                will(returnValue(sourceChildren));

                oneOf(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(sourceFileSo));

                oneOf(mockStore).removeObject(mockTransaction, sourceFilePath);

                oneOf(mockStore).removeObject(mockTransaction,
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
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceCollectionPath));

                exactly(2).of(mockReq).getHeader("Destination");
                will(returnValue(destCollectionPath));

                oneOf(mockReq).getServerName();
                will(returnValue("server_name"));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getPathInfo();
                will(returnValue(destCollectionPath));

                oneOf(mockReq).getServletPath();
                will(returnValue("servlet_path"));

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

                StoredObject destCollectionSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction,
                        destCollectionPath);
                will(returnValue(destCollectionSo));

                oneOf(mockRes).sendError(WebdavStatus.SC_PRECONDITION_FAILED);
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
    @Ignore
    public void testMovingOfaCollectionIfDestinationIsPresentAndOverwriteTrue()
            throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceCollectionPath));

                exactly(2).of(mockReq).getHeader("Destination");
                will(returnValue(overwritePath));

                oneOf(mockReq).getServerName();
                will(returnValue("server_name"));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getPathInfo();
                will(returnValue(overwritePath));

                oneOf(mockReq).getServletPath();
                will(returnValue("servlet_path"));

                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(sourceCollectionPath));

                oneOf(mockReq).getHeader("Overwrite");
                will(returnValue("T"));

                StoredObject sourceCollectionSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction,
                        sourceCollectionPath);
                will(returnValue(sourceCollectionSo));

                StoredObject destCollectionSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, overwritePath);
                will(returnValue(destCollectionSo));

                oneOf(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                oneOf(mockStore).getStoredObject(mockTransaction, overwritePath);
                will(returnValue(destCollectionSo));

                oneOf(mockStore).getChildrenNames(mockTransaction, overwritePath);
                will(returnValue(destChildren));

                StoredObject destFileSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction,
                        overwritePath + "/destFile");
                will(returnValue(destFileSo));

                oneOf(mockStore).removeObject(mockTransaction,
                        overwritePath + "/destFile");

                oneOf(mockStore).removeObject(mockTransaction, overwritePath);

                oneOf(mockStore).getStoredObject(mockTransaction,
                        sourceCollectionPath);
                will(returnValue(sourceCollectionSo));

                oneOf(mockStore).createFolder(mockTransaction, overwritePath);

                oneOf(mockReq).getHeader("Depth");
                will(returnValue(null));

                oneOf(mockStore).getChildrenNames(mockTransaction,
                        sourceCollectionPath);
                will(returnValue(sourceChildren));

                StoredObject sourceFileSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(sourceFileSo));

                oneOf(mockStore).createResource(mockTransaction,
                        overwritePath + "/sourceFile");

                oneOf(mockStore).getResourceContent(mockTransaction,
                        sourceFilePath);
                will(returnValue(dsis));

                oneOf(mockStore).setResourceContent(mockTransaction,
                        overwritePath + "/sourceFile", dsis, null, null);

                StoredObject movedSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction,
                        overwritePath + "/sourceFile");
                will(returnValue(movedSo));

                oneOf(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                oneOf(mockStore).getStoredObject(mockTransaction,
                        sourceCollectionPath);
                will(returnValue(sourceCollectionSo));

                sourceChildren = new String[] { "sourceFile" };

                oneOf(mockStore).getChildrenNames(mockTransaction,
                        sourceCollectionPath);
                will(returnValue(sourceChildren));

                oneOf(mockStore).getStoredObject(mockTransaction, sourceFilePath);
                will(returnValue(sourceFileSo));

                oneOf(mockStore).removeObject(mockTransaction, sourceFilePath);

                oneOf(mockStore).removeObject(mockTransaction,
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
