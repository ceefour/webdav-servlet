package net.sf.webdav.methods;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.webdav.ResourceLocks;
import net.sf.webdav.WebdavStore;

import org.jmock.Mock;
import org.jmock.MockObjectTestCase;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.util.Date;

public class DoGetTest extends MockObjectTestCase {

    public void testAccessOfaMissingPageResultsIn404() throws Exception {

        Mock mockStore = mock(WebdavStore.class);
        mockStore.expects(exactly(2)).method("isFolder").with(
                eq("/index.html")).will(returnValue(false));
        mockStore.expects(exactly(2)).method("objectExists").with(
                eq("/index.html")).will(returnValue(false));
        mockStore.expects(once()).method("isResource").with(eq("/index.html"))
                .will(returnValue(false));

        DoGet doGet = new DoGet((WebdavStore) mockStore.proxy(), null, null,
                new ResourceLocks(), 0, 0);
        Mock mockReq = mock(HttpServletRequest.class);

        mockReq.expects(once()).method("getAttribute").with(
                eq("javax.servlet.include.request_uri"))
                .will(returnValue(null));

        mockReq.expects(once()).method("getPathInfo").withNoArguments().will(
                returnValue("/index.html"));
        mockReq.expects(once()).method("getRequestURI").withNoArguments().will(
                returnValue("/index.html"));

        Mock mockRes = mock(HttpServletResponse.class);
        mockRes.expects(once()).method("sendError").with(eq(404),
                eq("/index.html"));

        doGet.execute((HttpServletRequest) mockReq.proxy(),
                (HttpServletResponse) mockRes.proxy(), true, "text/html");

    }


    public void testAccessOfaPageResultsInPage() throws Exception {

        Mock mockStore = mock(WebdavStore.class);
        mockStore.expects(once()).method("isFolder").with(
                eq("/index.html")).will(returnValue(false));

        // there is something extraneous about this objectExists() invocation. Whether true or
        // false it does not affect the outcome

        mockStore.expects(once()).method("objectExists").with(
                eq("/index.html")).will(returnValue(true));
        mockStore.expects(once()).method("isResource").with(eq("/index.html"))
                .will(returnValue(true));
        Date now = new Date(System.currentTimeMillis());
        mockStore.expects(once()).method("getLastModified").with(eq("/index.html"))
                .will(returnValue(now));
        mockStore.expects(once()).method("getResourceLength").with(eq("/index.html"))
                .will(returnValue(8L));
        ByteArrayInputStream bais = new ByteArrayInputStream(new byte[] {'<','h','e','l','l','o','/','>'});
        mockStore.expects(once()).method("getResourceContent").with(eq("/index.html"))
                .will(returnValue(bais));

        DoGet doGet = new DoGet((WebdavStore) mockStore.proxy(), null, null,
                new ResourceLocks(), 0, 0);
        Mock mockReq = mock(HttpServletRequest.class);

        mockReq.expects(once()).method("getAttribute").with(
                eq("javax.servlet.include.request_uri"))
                .will(returnValue(null));

        mockReq.expects(once()).method("getPathInfo").withNoArguments().will(
                returnValue("/index.html"));

        Mock mockRes = mock(HttpServletResponse.class);
        mockRes.expects(once()).method("setDateHeader").with(eq("last-modified"), eq(now.getTime()));
        mockRes.expects(once()).method("setContentType").with(eq("text/foo"));
        TestingOutputStream tos = new TestingOutputStream();
        mockRes.expects(once()).method("getOutputStream").withNoArguments().will(returnValue(tos));

        doGet.execute((HttpServletRequest) mockReq.proxy(),
                (HttpServletResponse) mockRes.proxy(), true, "text/foo");

        assertEquals("<hello/>", tos.toString());

    }


    public void testAccessOfaDirectoryResultsInRudimentaryChildList() throws Exception {

        Mock mockStore = mock(WebdavStore.class);
        mockStore.expects(exactly(2)).method("isFolder").with(
                eq("/foo/")).will(returnValue(true));
        mockStore.expects(once()).method("objectExists").with(
                eq("/foo/")).will(returnValue(false));
        mockStore.expects(once()).method("isResource").with(eq("/foo/"))
                .will(returnValue(false));
        mockStore.expects(once()).method("getChildrenNames").with(eq("/foo/"))
                .will(returnValue(new String[] {"AAA","BBB"}));


        DoGet doGet = new DoGet((WebdavStore) mockStore.proxy(), null, null,
                new ResourceLocks(), 0, 0);
        Mock mockReq = mock(HttpServletRequest.class);

        mockReq.expects(once()).method("getAttribute").with(
                eq("javax.servlet.include.request_uri"))
                .will(returnValue(null));

        mockReq.expects(once()).method("getPathInfo").withNoArguments().will(
                returnValue("/foo/"));

        Mock mockRes = mock(HttpServletResponse.class);
        TestingOutputStream tos = new TestingOutputStream();
        mockRes.expects(once()).method("getOutputStream").withNoArguments().will(returnValue(tos));

        doGet.execute((HttpServletRequest) mockReq.proxy(),
                (HttpServletResponse) mockRes.proxy(), true, "");

        assertEquals("Contents of this Folder:\nAAA\nBBB\n", tos.toString());

    }

    public void testAccessOfaDirectoryResultsInRedirectIfDefaultIndexFilePresent() throws Exception {

        Mock mockStore = mock(WebdavStore.class);
        mockStore.expects(once()).method("isFolder").with(
                eq("/foo/")).will(returnValue(true));

        DoGet doGet = new DoGet((WebdavStore) mockStore.proxy(), "/yeehaaa", null,
                new ResourceLocks(), 0, 0);
        Mock mockReq = mock(HttpServletRequest.class);

        mockReq.expects(once()).method("getAttribute").with(
                eq("javax.servlet.include.request_uri"))
                .will(returnValue(null));

        mockReq.expects(once()).method("getPathInfo").withNoArguments().will(
                returnValue("/foo/"));
        mockReq.expects(once()).method("getRequestURI").withNoArguments().will(
                returnValue("/foo"));

        Mock mockRes = mock(HttpServletResponse.class);
        mockRes.expects(once()).method("encodeRedirectURL").with(eq("/foo/yeehaaa"));
        mockRes.expects(once()).method("sendRedirect").with(eq(null));

        doGet.execute((HttpServletRequest) mockReq.proxy(),
                (HttpServletResponse) mockRes.proxy(), true, "text/html");


    }

    public void testAccessOfaMissingPageResultsInPossibleAlternatveTo404() throws Exception {

        Mock mockStore = mock(WebdavStore.class);
        mockStore.expects(once()).method("isFolder").with(
                eq("/index.html")).will(returnValue(false));
        mockStore.expects(once()).method("objectExists").with(
                eq("/index.html")).will(returnValue(false));
        mockStore.expects(once()).method("isResource").with(eq("/yeehaaa"))
                .will(returnValue(true));
        Date now = new Date(System.currentTimeMillis());
        mockStore.expects(once()).method("getLastModified").with(eq("/yeehaaa"))
                .will(returnValue(now));
        mockStore.expects(once()).method("getResourceLength").with(eq("/yeehaaa"))
                .will(returnValue(8L));
        ByteArrayInputStream bais = new ByteArrayInputStream(new byte[] {'<','h','e','l','l','o','/','>'});
        mockStore.expects(once()).method("getResourceContent").with(eq("/yeehaaa"))
                .will(returnValue(bais));

        DoGet doGet = new DoGet((WebdavStore) mockStore.proxy(), null, "/yeehaaa",
                new ResourceLocks(), 0, 0);
        Mock mockReq = mock(HttpServletRequest.class);

        mockReq.expects(once()).method("getAttribute").with(
                eq("javax.servlet.include.request_uri"))
                .will(returnValue(null));

        mockReq.expects(once()).method("getPathInfo").withNoArguments().will(
                returnValue("/index.html"));

        Mock mockRes = mock(HttpServletResponse.class);
        mockRes.expects(once()).method("setDateHeader").with(eq("last-modified"), eq(now.getTime()));
        mockRes.expects(once()).method("setContentType").with(eq("text/foo"));
        TestingOutputStream tos = new TestingOutputStream();
        mockRes.expects(once()).method("getOutputStream").withNoArguments().will(returnValue(tos));

        doGet.execute((HttpServletRequest) mockReq.proxy(),
                (HttpServletResponse) mockRes.proxy(), true, "text/foo");

        assertEquals("<hello/>", tos.toString());

    }


}
