package net.sf.webdav.methods;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.webdav.IMimeTyper;
import net.sf.webdav.ITransaction;
import net.sf.webdav.IWebdavStore;
import net.sf.webdav.StoredObject;
import net.sf.webdav.WebdavStatus;
import net.sf.webdav.locking.ResourceLocks;
import net.sf.webdav.testutil.MockTest;

import org.jmock.Expectations;
import org.junit.Before;
import org.junit.Test;

public class DoPropfindTest extends MockTest {
    IWebdavStore mockStore;
    IMimeTyper mockMimeTyper;
    HttpServletRequest mockReq;
    HttpServletResponse mockRes;
    ITransaction mockTransaction;
    PrintWriter printWriter;
    static byte[] resourceContent = new byte[] { '<', 'h', 'e', 'l', 'l', 'o',
            '/', '>' };

    @Before
    public void setUp() throws Exception {
        mockStore = _mockery.mock(IWebdavStore.class);
        mockMimeTyper = _mockery.mock(IMimeTyper.class);
        mockReq = _mockery.mock(HttpServletRequest.class);
        mockRes = _mockery.mock(HttpServletResponse.class);
        mockTransaction = _mockery.mock(ITransaction.class);
    }

    @Test
    public void doPropFindOnDirectory() throws Exception {
        final String path = "/";

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(path));

                oneOf(mockReq).getHeader("Depth");
                will(returnValue("infinity"));

                StoredObject rootSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(rootSo));

                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(path));

                oneOf(mockReq).getContentLength();
                will(returnValue(0));
                // no content, which means it is a allprop request

                oneOf(mockRes).setStatus(WebdavStatus.SC_MULTI_STATUS);

                oneOf(mockRes).setContentType("text/xml; charset=UTF-8");

                oneOf(mockRes).getWriter();
                will(returnValue(printWriter));

                oneOf(mockMimeTyper).getMimeType(mockTransaction, path);
                will(returnValue("text/xml; charset=UTF-8"));

                oneOf(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(rootSo));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getServletPath();
                will(returnValue(path));

                oneOf(mockStore).getChildrenNames(mockTransaction, path);
                will(returnValue(new String[] { "file1", "file2" }));

                StoredObject file1So = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, path + "file1");
                will(returnValue(file1So));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getServletPath();
                will(returnValue(path));

                oneOf(mockStore)
                        .getChildrenNames(mockTransaction, path + "file1");
                will(returnValue(new String[] {}));

                StoredObject file2So = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, path + "file2");
                will(returnValue(file2So));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getServletPath();
                will(returnValue(path));

                oneOf(mockStore)
                        .getChildrenNames(mockTransaction, path + "file2");
                will(returnValue(new String[] {}));
            }
        });

        DoPropfind doPropfind = new DoPropfind(mockStore, new ResourceLocks(),
                mockMimeTyper);
        doPropfind.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void doPropFindOnFile() throws Exception {
        final String path = "/testFile";

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(path));

                oneOf(mockReq).getHeader("Depth");
                will(returnValue("0"));

                StoredObject fileSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(fileSo));

                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(path));

                oneOf(mockReq).getContentLength();
                will(returnValue(0));
                // no content, which means it is a allprop request

                oneOf(mockRes).setStatus(WebdavStatus.SC_MULTI_STATUS);

                oneOf(mockRes).setContentType("text/xml; charset=UTF-8");

                oneOf(mockRes).getWriter();
                will(returnValue(printWriter));

                oneOf(mockMimeTyper).getMimeType(mockTransaction, path);
                will(returnValue("text/xml; charset=UTF-8"));

                oneOf(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(fileSo));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getServletPath();
                will(returnValue("/"));
            }
        });

        DoPropfind doPropfind = new DoPropfind(mockStore, new ResourceLocks(),
                mockMimeTyper);

        doPropfind.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void doPropFindOnNonExistingResource() throws Exception {
        final String path = "/notExists";

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(path));

                oneOf(mockReq).getHeader("Depth");
                will(returnValue("0"));

                StoredObject notExistingSo = null;

                oneOf(mockStore).getStoredObject(mockTransaction, path);
                will(returnValue(notExistingSo));

                oneOf(mockRes).setContentType("text/xml; charset=UTF-8");

                oneOf(mockReq).getRequestURI();
                will(returnValue(path));

                oneOf(mockRes).sendError(WebdavStatus.SC_NOT_FOUND, path);
            }
        });

        DoPropfind doPropfind = new DoPropfind(mockStore, new ResourceLocks(),
                mockMimeTyper);

        doPropfind.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

}
