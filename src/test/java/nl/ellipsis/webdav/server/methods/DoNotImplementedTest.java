package nl.ellipsis.webdav.server.methods;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.ellipsis.webdav.server.ITransaction;
import nl.ellipsis.webdav.server.IWebDAVStore;
import nl.ellipsis.webdav.server.methods.DoNotImplemented;
import nl.ellipsis.webdav.server.testutil.MockTest;

import org.jmock.Expectations;
import org.junit.BeforeClass;
import org.junit.Test;

public class DoNotImplementedTest extends MockTest {

	static IWebDAVStore mockStore;
	static HttpServletRequest mockReq;
	static HttpServletResponse mockRes;
	static ITransaction mockTransaction;

	@BeforeClass
	public static void setUp() throws Exception {
		mockStore = _mockery.mock(IWebDAVStore.class);
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
				oneOf(mockRes).sendError(HttpServletResponse.SC_FORBIDDEN);
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
				oneOf(mockRes).sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
			}
		});

		DoNotImplemented doNotImplemented = new DoNotImplemented(!readOnly);
		doNotImplemented.execute(mockTransaction, mockReq, mockRes);

		_mockery.assertIsSatisfied();
	}
}
