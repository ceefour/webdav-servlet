package net.sf.webdav.methods;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.webdav.ITransaction;
import net.sf.webdav.IWebdavStore;
import net.sf.webdav.WebDAVStatus;
import net.sf.webdav.testutil.MockTest;

import org.jmock.Expectations;
import org.junit.BeforeClass;
import org.junit.Test;

public class DoNotImplementedTest extends MockTest {

    static IWebdavStore mockStore;
    static HttpServletRequest mockReq;
    static HttpServletResponse mockRes;
    static ITransaction mockTransaction;

    @BeforeClass
    public static void setUp() throws Exception {
        mockStore = _mockery.mock(IWebdavStore.class);
        mockReq = _mockery.mock(HttpServletRequest.class);
        mockRes = _mockery.mock(HttpServletResponse.class);
        mockTransaction = _mockery.mock(ITransaction.class);
    }

    @Test
    public void testDoNotImplementedIfReadOnlyTrue() throws Exception {

        _mockery.checking(new Expectations() {
            {
                one(mockReq).getMethod();
                will(returnValue("notImplementedMethod"));
                one(mockRes).sendError(WebDAVStatus.SC_FORBIDDEN);
            }
        });

        DoNotImplemented doNotImplemented = new DoNotImplemented(readOnly);
        doNotImplemented.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testDoNotImplementedIfReadOnlyFalse() throws Exception {

        _mockery.checking(new Expectations() {
            {
                one(mockReq).getMethod();
                will(returnValue("notImplementedMethod"));
                one(mockRes).sendError(WebDAVStatus.SC_NOT_IMPLEMENTED);
            }
        });

        DoNotImplemented doNotImplemented = new DoNotImplemented(!readOnly);
        doNotImplemented.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }
}
