package nl.ellipsis.webdav.server.methods;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.ellipsis.webdav.server.IMimeTyper;
import nl.ellipsis.webdav.server.ITransaction;
import nl.ellipsis.webdav.server.IWebDAVStore;
import nl.ellipsis.webdav.server.StoredObject;
import nl.ellipsis.webdav.server.WebDAVConstants;
import nl.ellipsis.webdav.server.exceptions.LockFailedException;
import nl.ellipsis.webdav.server.locking.ResourceLocks;
import nl.ellipsis.webdav.server.methods.AbstractMethod;
import nl.ellipsis.webdav.server.methods.DoOptions;
import nl.ellipsis.webdav.server.testutil.MockTest;

import org.jmock.Expectations;
import org.junit.BeforeClass;
import org.junit.Test;

public class DoOptionsTest extends MockTest {

	static IWebDAVStore mockStore;
	static HttpServletRequest mockReq;
	static HttpServletResponse mockRes;
	static IMimeTyper mockMimeTyper;
	static ITransaction mockTransaction;
	static byte[] resourceContent = new byte[] { '<', 'h', 'e', 'l', 'l', 'o', '/', '>' };

	@BeforeClass
	public static void setUp() throws Exception {
		mockStore = _mockery.mock(IWebDAVStore.class);
		mockMimeTyper = _mockery.mock(IMimeTyper.class);
		mockReq = _mockery.mock(HttpServletRequest.class);
		mockRes = _mockery.mock(HttpServletResponse.class);
		mockTransaction = _mockery.mock(ITransaction.class);
	}

	@Test
	public void testOptionsOnExistingNode() throws IOException, LockFailedException {

		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue("/index.html"));

				oneOf(mockRes).addHeader("DAV", "1, 2");

				StoredObject indexSo = initFileStoredObject(resourceContent);

				oneOf(mockStore).getStoredObject(mockTransaction, "/index.html");
				will(returnValue(indexSo));

				oneOf(mockRes).addHeader("Allow", "OPTIONS, GET, HEAD, POST, DELETE, " + "TRACE, PROPPATCH, COPY, "
						+ "MOVE, LOCK, UNLOCK, PROPFIND");

				oneOf(mockRes).addHeader("MS-Author-Via", "DAV");
			}
		});

		DoOptions doOptions = new DoOptions(mockStore, new ResourceLocks());
		doOptions.execute(mockTransaction, mockReq, mockRes);

		_mockery.assertIsSatisfied();
	}

	@Test
	public void testOptionsOnNonExistingNode() throws IOException, LockFailedException {

		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_PATH_INFO);
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
