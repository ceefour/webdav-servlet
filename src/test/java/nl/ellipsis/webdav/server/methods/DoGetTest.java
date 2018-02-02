package nl.ellipsis.webdav.server.methods;

import java.io.ByteArrayInputStream;
import java.util.Locale;

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
import nl.ellipsis.webdav.server.methods.DoGet;
import nl.ellipsis.webdav.server.testutil.MockTest;
import nl.ellipsis.webdav.server.testutil.TestingOutputStream;

import org.jmock.Expectations;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.DelegatingServletInputStream;

public class DoGetTest extends MockTest {

	static IWebDAVStore mockStore;
	static IMimeTyper mockMimeTyper;
	static HttpServletRequest mockReq;
	static HttpServletResponse mockRes;
	static ITransaction mockTransaction;
	static TestingOutputStream tos = new TestingOutputStream();;
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
	public void testAccessOfaMissingPageResultsIn404() throws Exception {

		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue("/index.html"));

				StoredObject indexSo = null;

				exactly(2).of(mockStore).getStoredObject(mockTransaction, "/index.html");
				will(returnValue(indexSo));

				oneOf(mockReq).getRequestURI();
				will(returnValue("/index.html"));

				oneOf(mockRes).sendError(HttpServletResponse.SC_NOT_FOUND, "/index.html");

				oneOf(mockRes).setStatus(HttpServletResponse.SC_NOT_FOUND);
			}
		});

		DoGet doGet = new DoGet(mockStore, null, null, new ResourceLocks(), mockMimeTyper, 0);

		doGet.execute(mockTransaction, mockReq, mockRes);

		_mockery.assertIsSatisfied();
	}

	@Test
	public void testAccessOfaPageResultsInPage() throws Exception {

		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue("/index.html"));

				StoredObject indexSo = initFileStoredObject(resourceContent);

				oneOf(mockStore).getStoredObject(mockTransaction, "/index.html");
				will(returnValue(indexSo));

				oneOf(mockReq).getHeader(javax.ws.rs.core.HttpHeaders.IF_NONE_MATCH);
				will(returnValue(null));

				oneOf(mockRes).setDateHeader("last-modified", indexSo.getLastModified().getTime());

				oneOf(mockRes).addHeader(with(any(String.class)), with(any(String.class)));

				oneOf(mockMimeTyper).getMimeType(mockTransaction, "/index.html");
				will(returnValue("text/foo"));

				oneOf(mockRes).setContentType("text/foo");

				StoredObject so = initFileStoredObject(resourceContent);

				oneOf(mockStore).getStoredObject(mockTransaction, "/index.html");
				will(returnValue(so));

				oneOf(mockRes).getOutputStream();
				will(returnValue(tos));

				oneOf(mockStore).getResourceContent(mockTransaction, "/index.html");
				will(returnValue(dsis));
			}
		});

		DoGet doGet = new DoGet(mockStore, null, null, new ResourceLocks(), mockMimeTyper, 0);

		doGet.execute(mockTransaction, mockReq, mockRes);

		assertEquals("<hello/>", tos.toString());

		_mockery.assertIsSatisfied();
	}

	@Test
	public void testAccessOfaDirectoryResultsInRudimentaryChildList() throws Exception {

		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue("/foo/"));

				StoredObject fooSo = initFolderStoredObject();
				StoredObject aaa = initFolderStoredObject();
				StoredObject bbb = initFolderStoredObject();

				oneOf(mockStore).getStoredObject(mockTransaction, "/foo");
				will(returnValue(fooSo));

				oneOf(mockReq).getHeader(javax.ws.rs.core.HttpHeaders.IF_NONE_MATCH);
				will(returnValue(null));

				oneOf(mockStore).getStoredObject(mockTransaction, "/foo");
				will(returnValue(fooSo));

				oneOf(mockReq).getLocale();
				will(returnValue(Locale.GERMAN));

				oneOf(mockRes).setContentType("text/html");
				oneOf(mockRes).setCharacterEncoding("UTF8");

				tos = new TestingOutputStream();

				oneOf(mockRes).getOutputStream();
				will(returnValue(tos)); 

				oneOf(mockStore).getChildrenNames(mockTransaction, "/foo");
				will(returnValue(new String[] { "AAA", "BBB" }));
				
				oneOf(mockReq).getContextPath();
				will(returnValue("/"));
				oneOf(mockReq).getServletPath();
				will(returnValue("/"));
				
				oneOf(mockStore).getStoredObject(mockTransaction, "/foo/AAA");
				will(returnValue(aaa));

				oneOf(mockStore).getStoredObject(mockTransaction, "/foo/BBB");
				will(returnValue(bbb));

			}
		});

		DoGet doGet = new DoGet(mockStore, null, null, new ResourceLocks(), mockMimeTyper, 0);

		doGet.execute(mockTransaction, mockReq, mockRes);

		assertTrue(tos.toString().length() > 0);

		_mockery.assertIsSatisfied();
	}

	@Test
	public void testAccessOfaDirectoryResultsInRedirectIfDefaultIndexFilePresent() throws Exception {

		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_PATH_INFO);
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

		DoGet doGet = new DoGet(mockStore, "/indexFile", null, new ResourceLocks(), mockMimeTyper, 0);

		doGet.execute(mockTransaction, mockReq, mockRes);

		_mockery.assertIsSatisfied();
	}

	@Test
	public void testAccessOfaMissingPageResultsInPossibleAlternatveTo404() throws Exception {

		_mockery.checking(new Expectations() {
			{
				oneOf(mockReq).getAttribute(WebDAVConstants.HttpRequestParam.INCLUDE_PATH_INFO);
				will(returnValue(null));

				oneOf(mockReq).getPathInfo();
				will(returnValue("/index.html"));

				StoredObject indexSo = null;

				oneOf(mockStore).getStoredObject(mockTransaction, "/index.html");
				will(returnValue(indexSo));

				StoredObject alternativeSo = initFileStoredObject(resourceContent);

				oneOf(mockStore).getStoredObject(mockTransaction, "/alternative");
				will(returnValue(alternativeSo));

				oneOf(mockReq).getHeader(javax.ws.rs.core.HttpHeaders.IF_NONE_MATCH);
				will(returnValue(null));

				oneOf(mockRes).setDateHeader("last-modified", alternativeSo.getLastModified().getTime());

				oneOf(mockRes).addHeader(with(any(String.class)), with(any(String.class)));

				oneOf(mockMimeTyper).getMimeType(mockTransaction, "/alternative");
				will(returnValue("text/foo"));

				oneOf(mockRes).setContentType("text/foo");

				oneOf(mockStore).getStoredObject(mockTransaction, "/alternative");
				will(returnValue(alternativeSo));

				tos = new TestingOutputStream();
				tos.write(resourceContent);

				oneOf(mockRes).getOutputStream();
				will(returnValue(tos));

				oneOf(mockStore).getResourceContent(mockTransaction, "/alternative");
				will(returnValue(dsis));

				oneOf(mockRes).setStatus(HttpServletResponse.SC_NOT_FOUND);
			}
		});

		DoGet doGet = new DoGet(mockStore, null, "/alternative", new ResourceLocks(), mockMimeTyper, 0);

		doGet.execute(mockTransaction, mockReq, mockRes);

		assertEquals("<hello/>", tos.toString());

		_mockery.assertIsSatisfied();
	}

}
