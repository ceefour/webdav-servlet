package net.sf.webdav.methods;

import java.io.ByteArrayInputStream;
import java.util.Locale;

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
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.DelegatingServletInputStream;

public class DoGetTest extends MockTest {

    static IWebdavStore mockStore;
    static IMimeTyper mockMimeTyper;
    static HttpServletRequest mockReq;
    static HttpServletResponse mockRes;
    static ITransaction mockTransaction;
    static TestingOutputStream tos = new TestingOutputStream();;
    static byte[] resourceContent = new byte[] { '<', 'h', 'e', 'l', 'l', 'o',
            '/', '>' };
    static ByteArrayInputStream bais = new ByteArrayInputStream(resourceContent);
    static DelegatingServletInputStream dsis = new DelegatingServletInputStream(
            bais);

    @BeforeClass
    public static void setUp() throws Exception {
        mockStore = _mockery.mock(IWebdavStore.class);
        mockMimeTyper = _mockery.mock(IMimeTyper.class);
        mockReq = _mockery.mock(HttpServletRequest.class);
        mockRes = _mockery.mock(HttpServletResponse.class);
        mockTransaction = _mockery.mock(ITransaction.class);
    }

    @Test
    public void testAccessOfaMissingPageResultsIn404() throws Exception {

        _mockery.checking(new Expectations() {
            {
                one(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue("/index.html"));

                StoredObject indexSo = null;

                exactly(2).of(mockStore).getStoredObject(mockTransaction,
                        "/index.html");
                will(returnValue(indexSo));

                one(mockReq).getRequestURI();
                will(returnValue("/index.html"));

                one(mockRes)
                        .sendError(WebdavStatus.SC_NOT_FOUND, "/index.html");

                one(mockRes).setStatus(WebdavStatus.SC_NOT_FOUND);
            }
        });

        DoGet doGet = new DoGet(mockStore, null, null, new ResourceLocks(),
                mockMimeTyper, 0);

        doGet.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testAccessOfaPageResultsInPage() throws Exception {

        _mockery.checking(new Expectations() {
            {
                one(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue("/index.html"));

                StoredObject indexSo = initFileStoredObject(resourceContent);

                one(mockStore).getStoredObject(mockTransaction, "/index.html");
                will(returnValue(indexSo));

                one(mockReq).getHeader("If-None-Match");
                will(returnValue(null));

                one(mockRes).setDateHeader("last-modified",
                        indexSo.getLastModified().getTime());

                one(mockRes).addHeader(with(any(String.class)),
                        with(any(String.class)));

                one(mockMimeTyper).getMimeType("/index.html");
                will(returnValue("text/foo"));

                one(mockRes).setContentType("text/foo");

                StoredObject so = initFileStoredObject(resourceContent);

                one(mockStore).getStoredObject(mockTransaction, "/index.html");
                will(returnValue(so));

                one(mockRes).getOutputStream();
                will(returnValue(tos));

                one(mockStore).getResourceContent(mockTransaction,
                        "/index.html");
                will(returnValue(dsis));
            }
        });

        DoGet doGet = new DoGet(mockStore, null, null, new ResourceLocks(),
                mockMimeTyper, 0);

        doGet.execute(mockTransaction, mockReq, mockRes);

        assertEquals("<hello/>", tos.toString());

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testAccessOfaDirectoryResultsInRudimentaryChildList()
            throws Exception {

        _mockery.checking(new Expectations() {
            {
                one(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue("/foo/"));

                StoredObject fooSo = initFolderStoredObject();
                StoredObject aaa = initFolderStoredObject();
                StoredObject bbb = initFolderStoredObject();

                one(mockStore).getStoredObject(mockTransaction, "/foo/");
                will(returnValue(fooSo));

                one(mockReq).getHeader("If-None-Match");
                will(returnValue(null));

                one(mockStore).getStoredObject(mockTransaction, "/foo/");
                will(returnValue(fooSo));

                one(mockReq).getLocale();
                will(returnValue(Locale.GERMAN));
                
                one(mockRes).setContentType("text/html");
				one(mockRes).setCharacterEncoding("UTF8");
                
                tos = new TestingOutputStream();

                one(mockRes).getOutputStream();
                will(returnValue(tos));

                one(mockStore).getChildrenNames(mockTransaction, "/foo/");
                will(returnValue(new String[] { "AAA", "BBB" }));
                
                one(mockStore).getStoredObject(mockTransaction, "/foo//AAA");
                will(returnValue(aaa));

                one(mockStore).getStoredObject(mockTransaction, "/foo//BBB");
				will(returnValue(bbb));

            }
        });

        DoGet doGet = new DoGet(mockStore, null, null, new ResourceLocks(),
                mockMimeTyper, 0);

        doGet.execute(mockTransaction, mockReq, mockRes);

        assertTrue(tos.toString().length() > 0);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testAccessOfaDirectoryResultsInRedirectIfDefaultIndexFilePresent()
            throws Exception {

        _mockery.checking(new Expectations() {
            {
                one(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue("/foo/"));

                StoredObject fooSo = initFolderStoredObject();

                one(mockStore).getStoredObject(mockTransaction, "/foo/");
                will(returnValue(fooSo));

                one(mockReq).getRequestURI();
                will(returnValue("/foo/"));

                one(mockRes).encodeRedirectURL("/foo//indexFile");

                one(mockRes).sendRedirect("");
            }
        });

        DoGet doGet = new DoGet(mockStore, "/indexFile", null,
                new ResourceLocks(), mockMimeTyper, 0);

        doGet.execute(mockTransaction, mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testAccessOfaMissingPageResultsInPossibleAlternatveTo404()
            throws Exception {

        _mockery.checking(new Expectations() {
            {
                one(mockReq).getAttribute("javax.servlet.include.request_uri");
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue("/index.html"));

                StoredObject indexSo = null;

                one(mockStore).getStoredObject(mockTransaction, "/index.html");
                will(returnValue(indexSo));

                StoredObject alternativeSo = initFileStoredObject(resourceContent);

                one(mockStore).getStoredObject(mockTransaction, "/alternative");
                will(returnValue(alternativeSo));

                one(mockReq).getHeader("If-None-Match");
                will(returnValue(null));

                one(mockRes).setDateHeader("last-modified",
                        alternativeSo.getLastModified().getTime());

                one(mockRes).addHeader(with(any(String.class)),
                        with(any(String.class)));

                one(mockMimeTyper).getMimeType("/alternative");
                will(returnValue("text/foo"));

                one(mockRes).setContentType("text/foo");

                one(mockStore).getStoredObject(mockTransaction, "/alternative");
                will(returnValue(alternativeSo));

                tos = new TestingOutputStream();
                tos.write(resourceContent);

                one(mockRes).getOutputStream();
                will(returnValue(tos));

                one(mockStore).getResourceContent(mockTransaction,
                        "/alternative");
                will(returnValue(dsis));

                one(mockRes).setStatus(WebdavStatus.SC_NOT_FOUND);
            }
        });

        DoGet doGet = new DoGet(mockStore, null, "/alternative",
                new ResourceLocks(), mockMimeTyper, 0);

        doGet.execute(mockTransaction, mockReq, mockRes);

        assertEquals("<hello/>", tos.toString());

        _mockery.assertIsSatisfied();
    }

}
