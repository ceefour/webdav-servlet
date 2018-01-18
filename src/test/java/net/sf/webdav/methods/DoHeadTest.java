package net.sf.webdav.methods;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.webdav.IMimeTyper;
import net.sf.webdav.ITransaction;
import net.sf.webdav.IWebDAVStore;
import net.sf.webdav.StoredObject;
import net.sf.webdav.WebDAVStatus;
import net.sf.webdav.locking.ResourceLocks;
import net.sf.webdav.testutil.MockTest;
import net.sf.webdav.testutil.TestingOutputStream;

import org.jmock.Expectations;
import org.junit.BeforeClass;
import org.junit.Test;

public class DoHeadTest extends MockTest {

	static IWebDAVStore mockStore;
	static IMimeTyper mockMimeTyper;
	static HttpServletRequest mockReq;
	static HttpServletResponse mockRes;
	static TestingOutputStream tos;
	static ITransaction mockTransaction;
	static byte[] resourceContent = new byte[] { '<', 'h', 'e', 'l', 'l', 'o', '/', '>' };

	@BeforeClass
	public static void setUp() throws Exception {
		mockStore = _mockery.mock(IWebDAVStore.class);
		mockMimeTyper = _mockery.mock(IMimeTyper.class);
		mockReq = _mockery.mock(HttpServletRequest.class);
		mockRes = _mockery.mock(HttpServletResponse.class);
		tos = new TestingOutputStream();
		mockTransaction = _mockery.mock(ITransaction.class);
	}

	@Test
	public void testAccessOfaMissingPageResultsIn404() throws Exception {

		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(AbstractMethod.ATTR_INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue("/index.html"));

				StoredObject indexSo = null;

				oneOf(mockStore).getStoredObject(mockTransaction, "/index.html");
				will(returnValue(indexSo));

				oneOf(mockRes).setStatus(WebDAVStatus.SC_NOT_FOUND);
			}
		});

		DoHead doHead = new DoHead(mockStore, null, null, new ResourceLocks(), mockMimeTyper, 0);
		doHead.execute(mockTransaction, mockReq, mockRes);

		_mockery.assertIsSatisfied();
	}

	@Test
	public void testAccessOfaPageResultsInPage() throws Exception {

		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(AbstractMethod.ATTR_INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue("/index.html"));

				StoredObject indexSo = initFileStoredObject(resourceContent);

				oneOf(mockStore).getStoredObject(mockTransaction, "/index.html");
				will(returnValue(indexSo));

				oneOf(mockReq).getHeader("If-None-Match");
				will(returnValue(null));

				oneOf(mockRes).setDateHeader("last-modified", indexSo.getLastModified().getTime());

				oneOf(mockRes).addHeader(with(any(String.class)), with(any(String.class)));

				oneOf(mockMimeTyper).getMimeType(mockTransaction, "/index.html");
				will(returnValue("text/foo"));

				oneOf(mockRes).setContentType("text/foo");
			}
		});

		DoHead doHead = new DoHead(mockStore, null, null, new ResourceLocks(), mockMimeTyper, 0);

		doHead.execute(mockTransaction, mockReq, mockRes);

		_mockery.assertIsSatisfied();
	}

	@Test
	public void testAccessOfaDirectoryResultsInRedirectIfDefaultIndexFilePresent() throws Exception {

		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(AbstractMethod.ATTR_INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue("/foo/"));

				StoredObject fooSo = initFolderStoredObject();

				oneOf(mockStore).getStoredObject(mockTransaction, "/foo");
				will(returnValue(fooSo));

				oneOf(mockReq).getRequestURI();
				will(returnValue("/foo/"));

				oneOf(mockRes).encodeRedirectURL("/foo/indexFile");

				oneOf(mockRes).sendRedirect("");
			}
		});

		DoHead doHead = new DoHead(mockStore, "/indexFile", null, new ResourceLocks(), mockMimeTyper, 0);

		doHead.execute(mockTransaction, mockReq, mockRes);

		_mockery.assertIsSatisfied();
	}

}
