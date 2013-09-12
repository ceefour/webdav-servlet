package net.sf.webdav;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import net.sf.webdav.testutil.MockPrincipal;
import net.sf.webdav.testutil.MockTest;

import org.jmock.Expectations;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

public class WebdavServletTest extends MockTest {

    // private static WebdavServlet _servlet = new WebdavServlet();
    ServletConfig servletConfig;
    ServletContext servletContext;
    // static HttpServletRequest mockeryReq;
    // static HttpServletResponse mockRes;
    IWebdavStore mockStore;

    MockServletConfig mockServletConfig;
    MockServletContext mockServletContext;
    MockHttpServletRequest mockReq;
    MockHttpServletResponse mockRes;
    MockHttpSession mockHttpSession;
    MockPrincipal mockPrincipal;
    ITransaction mockTransaction;

    static boolean readOnly = true;
    static byte[] resourceContent = new byte[] { '<', 'h', 'e', 'l', 'l', 'o',
            '/', '>' };
    static String dftIndexFile = "/index.html";
    static String insteadOf404 = "/insteadOf404";

    @Before
    public void setUp() throws Exception {
        servletConfig = _mockery.mock(ServletConfig.class);
        servletContext = _mockery.mock(ServletContext.class);
        mockStore = _mockery.mock(IWebdavStore.class);

        mockServletConfig = new MockServletConfig(mockServletContext);
        mockHttpSession = new MockHttpSession(mockServletContext);
        mockServletContext = new MockServletContext();
        mockReq = new MockHttpServletRequest(mockServletContext);
        mockRes = new MockHttpServletResponse();

        mockPrincipal = new MockPrincipal("Admin", new String[] { "Admin",
                "Manager" });

        mockTransaction = _mockery.mock(ITransaction.class);
    }

    @Test
    public void testInit() throws Exception {
    	
        _mockery.checking(new Expectations() {
        });

        WebDavServletBean servlet = new WebdavServlet();
        servlet.init(mockStore, null, dftIndexFile, insteadOf404, 1, true);

        _mockery.assertIsSatisfied();
    }

    // Test successes in eclipse, but fails in "mvn test"
    // first three expectations aren't successful with "mvn test"
    @Test
    public void testInitGenericServlet() throws Exception {

        _mockery.checking(new Expectations() {
            {
                allowing(servletConfig).getServletContext();
                will(returnValue(mockServletContext));

                allowing(servletConfig).getServletName();
                will(returnValue("webdav-servlet"));

                allowing(servletContext).log("webdav-servlet: init");

                oneOf(servletConfig).getInitParameter(
                        "ResourceHandlerImplementation");
                will(returnValue(""));

                oneOf(servletConfig).getInitParameter(
                		"LockingListener");
                will(returnValue(""));

                oneOf(servletConfig).getInitParameter("rootpath");
                will(returnValue("./target/tmpTestData/"));

                exactly(2).of(servletConfig).getInitParameter(
                        "lazyFolderCreationOnPut");
                will(returnValue("1"));

                oneOf(servletConfig).getInitParameter("default-index-file");
                will(returnValue("index.html"));

                oneOf(servletConfig).getInitParameter("instead-of-404");
                will(returnValue(""));

                exactly(2).of(servletConfig).getInitParameter(
                        "no-content-length-headers");
                will(returnValue("0"));
            }
        });

        WebDavServletBean servlet = new WebdavServlet();

        servlet.init(servletConfig);

        _mockery.assertIsSatisfied();
    }

    @Test
    public void testService() throws Exception {

        mockServletConfig.addInitParameter("ResourceHandlerImplementation", "");
        mockServletConfig.addInitParameter("rootpath", "./target/tmpTestData");
        mockServletConfig.addInitParameter("lazyFolderCreationOnPut", "1");
        mockServletConfig.addInitParameter("default-index-file", dftIndexFile);
        mockServletConfig.addInitParameter("instead-of-404", insteadOf404);
        mockServletConfig.addInitParameter("no-content-length-headers", "0");

        // StringTokenizer headers = new StringTokenizer(
        // "Host Depth Content-Type Content-Length");
        mockReq.setMethod("PUT");
        mockReq.setAttribute("javax.servlet.include.request_uri", null);
        mockReq.setPathInfo("/aPath/toAFile");
        mockReq.setRequestURI("/aPath/toAFile");
        mockReq.addHeader("Host", "www.foo.bar");
        mockReq.addHeader("Depth", "0");
        mockReq.addHeader("Content-Type", "text/xml");
        mockReq.addHeader("Content-Length", "1234");
        mockReq.addHeader("User-Agent", "...some Client with WebDAVFS...");

        mockReq.setSession(mockHttpSession);
        mockPrincipal = new MockPrincipal("Admin", new String[] { "Admin",
                "Manager" });
        mockReq.setUserPrincipal(mockPrincipal);
        mockReq.addUserRole("Admin");
        mockReq.addUserRole("Manager");

        mockReq.setContent(resourceContent);

        _mockery.checking(new Expectations() {
            {

            }
        });

        WebDavServletBean servlet = new WebdavServlet();

        servlet.init(mockServletConfig);

        servlet.service(mockReq, mockRes);

        _mockery.assertIsSatisfied();
    }
}
