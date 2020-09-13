package nl.ellipsis.webdav.server.methods;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.ellipsis.webdav.HttpHeaders;
import nl.ellipsis.webdav.server.IMimeTyper;
import nl.ellipsis.webdav.server.ITransaction;
import nl.ellipsis.webdav.server.IWebDAVStore;
import nl.ellipsis.webdav.server.StoredObject;
import nl.ellipsis.webdav.server.WebDAVConstants;
import nl.ellipsis.webdav.server.locking.ResourceLocks;
import nl.ellipsis.webdav.server.methods.AbstractMethod;
import nl.ellipsis.webdav.server.methods.DoProppatch;
import nl.ellipsis.webdav.server.testutil.MockTest;

import org.jmock.Expectations;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.DelegatingServletInputStream;

public class DoProppatchTest extends MockTest {
	static IWebDAVStore mockStore;
	static IMimeTyper mockMimeTyper;
	static HttpServletRequest mockReq;
	static HttpServletResponse mockRes;
	static ITransaction mockTransaction;
	static byte[] resourceContent = new byte[] { '<', 'h', 'e', 'l', 'l', 'o', '/', '>' };
	static ByteArrayInputStream bais = new ByteArrayInputStream(resourceContent);
	static DelegatingServletInputStream dsis = new DelegatingServletInputStream(bais);

	@BeforeClass
	public static void setUp() throws Exception {
		mockStore = _mockery.mock(IWebDAVStore.class);
		mockMimeTyper = _mockery.mock(IMimeTyper.class);
		mockReq = _mockery.mock(HttpServletRequest.class);
		mockRes = _mockery.mock(HttpServletResponse.class);
		mockTransaction = _mockery.mock(ITransaction.class);
	}

	@Test
	public void doProppatchIfReadOnly() throws Exception {
		final String path = "/readinly";
		
		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(path));
				
				oneOf(mockRes).sendError(HttpServletResponse.SC_FORBIDDEN);
			}
		});

		DoProppatch doProppatch = new DoProppatch(mockStore, new ResourceLocks(), readOnly);

		doProppatch.execute(mockTransaction, mockReq, mockRes);

		_mockery.assertIsSatisfied();
	}

	@Test
	public void doProppatchOnNonExistingResource() throws Exception {

		final String path = "/notExists";

		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(path));

				StoredObject notExistingSo = null;

				oneOf(mockStore).getStoredObject(mockTransaction, path);
				will(returnValue(notExistingSo));

				oneOf(mockRes).sendError(HttpServletResponse.SC_NOT_FOUND);
			}
		});

		DoProppatch doProppatch = new DoProppatch(mockStore, new ResourceLocks(), !readOnly);

		doProppatch.execute(mockTransaction, mockReq, mockRes);

		_mockery.assertIsSatisfied();
	}

	@Test
	public void doProppatchOnRequestWithNoContent() throws Exception {

		final String path = "/testFile";

		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(path));

				StoredObject testFileSo = initFileStoredObject(null);

				oneOf(mockStore).getStoredObject(mockTransaction, path);
				will(returnValue(testFileSo));

				oneOf(mockReq).getHeader(HttpHeaders.IF);
				will(returnValue(null));
				
				oneOf(mockReq).getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(path));

				oneOf(mockReq).getContentLength();
				will(returnValue(0));

				oneOf(mockRes).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			}
		});

		DoProppatch doProppatch = new DoProppatch(mockStore, new ResourceLocks(), !readOnly);

		doProppatch.execute(mockTransaction, mockReq, mockRes);

		_mockery.assertIsSatisfied();
	}

	@Test
	public void doProppatchOnResource() throws Exception {

		final String path = "/testFile";
		final PrintWriter pw = new PrintWriter(tmpFolder + "/XMLTestFile");

		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(path));

				StoredObject testFileSo = initFileStoredObject(resourceContent);

				oneOf(mockStore).getStoredObject(mockTransaction, path);
				will(returnValue(testFileSo));
				
				oneOf(mockReq).getHeader(HttpHeaders.IF);
				will(returnValue(null));

				oneOf(mockReq).getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(path));

				oneOf(mockReq).getContentLength();
				will(returnValue(8));

				oneOf(mockReq).getInputStream();
				will(returnValue(dsis));

				oneOf(mockRes).setStatus(HttpStatus.MULTI_STATUS.value());

				oneOf(mockRes).setContentType("text/xml; charset=UTF-8");

				oneOf(mockRes).getWriter();
				will(returnValue(pw));

				oneOf(mockReq).getContextPath();
				will(returnValue(""));
			}
		});

		DoProppatch doProppatch = new DoProppatch(mockStore, new ResourceLocks(), !readOnly);

		doProppatch.execute(mockTransaction, mockReq, mockRes);

		_mockery.assertIsSatisfied();
	}

}
