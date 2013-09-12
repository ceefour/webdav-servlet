package net.sf.webdav.methods;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.webdav.IMimeTyper;
import net.sf.webdav.ITransaction;
import net.sf.webdav.IWebdavStore;
import net.sf.webdav.StoredObject;
import net.sf.webdav.exceptions.LockFailedException;
import net.sf.webdav.locking.ResourceLocks;
import net.sf.webdav.testutil.MockTest;

import org.jmock.Expectations;
import org.junit.Before;
import org.junit.Test;

public class DoOptionsTest extends MockTest {

    IWebdavStore mockStore;
    HttpServletRequest mockReq;
    HttpServletResponse mockRes;
    IMimeTyper mockMimeTyper;
    ITransaction mockTransaction;
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
    public void testOptionsOnExistingNode() throws IOException,
            LockFailedException {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue("/index.html"));

                oneOf(mockRes).addHeader("DAV", "1, 2");

                StoredObject indexSo = initFileStoredObject(resourceContent);

                oneOf(mockStore).getStoredObject(mockTransaction, "/index.html");
                will(returnValue(indexSo));

                oneOf(mockRes).addHeader(
                        "Allow",
                        "OPTIONS, GET, HEAD, POST, DELETE, "
                                + "TRACE, PROPPATCH, COPY, "
                                + "MOVE, LOCK, UNLOCK, PROPFIND");

                oneOf(mockRes).addHeader("MS-Author-Via", "DAV");
            }
        });

        DoOptions doOptions = new DoOptions(mockStore, new ResourceLocks());
        doOptions.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testOptionsOnNonExistingNode() throws IOException,
            LockFailedException {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue("/index.html"));

                oneOf(mockRes).addHeader("DAV", "1, 2");

                StoredObject indexSo = null;

                oneOf(mockStore).getStoredObject(mockTransaction, "/index.html");
                will(returnValue(indexSo));

                oneOf(mockRes).addHeader("Allow", "OPTIONS, MKCOL, PUT");

                oneOf(mockRes).addHeader("MS-Author-Via", "DAV");
            }
        });

        DoOptions doOptions = new DoOptions(mockStore, new ResourceLocks());
        doOptions.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

}
