package net.sf.webdav.methods;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.webdav.IMimeTyper;
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

		_mockery.checking(new Expectations() {
			{
				oneOf(mockRes).sendError(WebDAVStatus.SC_FORBIDDEN);
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
				oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(path));

				StoredObject notExistingSo = null;

				oneOf(mockStore).getStoredObject(mockTransaction, path);
				will(returnValue(notExistingSo));

				oneOf(mockRes).sendError(WebDAVStatus.SC_NOT_FOUND);
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
				oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(path));

				StoredObject testFileSo = initFileStoredObject(resourceContent);

				oneOf(mockStore).getStoredObject(mockTransaction, path);
				will(returnValue(testFileSo));

				oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(path));

				oneOf(mockReq).getContentLength();
				will(returnValue(0));

				oneOf(mockRes).sendError(WebDAVStatus.SC_INTERNAL_SERVER_ERROR);
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
				oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(path));

				StoredObject testFileSo = initFileStoredObject(resourceContent);

				oneOf(mockStore).getStoredObject(mockTransaction, path);
				will(returnValue(testFileSo));

				oneOf(mockReq).getAttribute("javax.servlet.include.request_uri");
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue(path));

				oneOf(mockReq).getContentLength();
				will(returnValue(8));

				oneOf(mockReq).getInputStream();
				will(returnValue(dsis));

				oneOf(mockRes).setStatus(WebDAVStatus.SC_MULTI_STATUS);

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
