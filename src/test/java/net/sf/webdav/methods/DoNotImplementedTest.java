package net.sf.webdav.methods;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.webdav.ITransaction;
import net.sf.webdav.IWebdavStore;
import net.sf.webdav.WebdavStatus;
import net.sf.webdav.testutil.MockTest;

import org.jmock.Expectations;
import org.junit.Before;
import org.junit.Test;

public class DoNotImplementedTest extends MockTest {

    IWebdavStore mockStore;
    HttpServletRequest mockReq;
    HttpServletResponse mockRes;
    ITransaction mockTransaction;

    @Before
    public void setUp() throws Exception {
        mockStore = _mockery.mock(IWebdavStore.class);
        mockReq = _mockery.mock(HttpServletRequest.class);
        mockRes = _mockery.mock(HttpServletResponse.class);
        mockTransaction = _mockery.mock(ITransaction.class);
    }

    @Test
    public void testDoNotImplementedIfReadOnlyTrue() throws Exception {

        _mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getMethod();
                will(returnValue("notImplementedMethod"));
                oneOf(mockRes).sendError(WebdavStatus.SC_FORBIDDEN);
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
                oneOf(mockReq).getMethod();
                will(returnValue("notImplementedMethod"));
                oneOf(mockRes).sendError(WebdavStatus.SC_NOT_IMPLEMENTED);
            }
        });

        DoNotImplemented doNotImplemented = new DoNotImplemented(!readOnly);
        doNotImplemented.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }
}
