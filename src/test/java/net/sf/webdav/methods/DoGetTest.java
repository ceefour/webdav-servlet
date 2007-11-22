package net.sf.webdav.methods;

import java.io.ByteArrayInputStream;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.webdav.MimeTyper;
import net.sf.webdav.ResourceLocks;
import net.sf.webdav.WebdavStore;

import org.jmock.Mock;
import org.jmock.MockObjectTestCase;

public class DoGetTest extends MockObjectTestCase {

    Mock mockStore;
    Mock mockMimeTyper;
    Mock mockReq;
    Mock mockRes;

    protected void setUp() throws Exception {
        mockStore = mock(WebdavStore.class);
        mockMimeTyper = mock(MimeTyper.class);
        mockReq = mock(HttpServletRequest.class);
        mockRes = mock(HttpServletResponse.class);
    }

    public void testAccessOfaMissingPageResultsIn404() throws Exception {

        mockStore.expects(exactly(2)).method("isFolder").with(
                eq("/index.html")).will(returnValue(false));
        mockStore.expects(exactly(2)).method("objectExists").with(
                eq("/index.html")).will(returnValue(false));
        mockStore.expects(once()).method("isResource").with(eq("/index.html"))
                .will(returnValue(false));

        DoGet doGet = new DoGet((WebdavStore) mockStore.proxy(), null, null,
                new ResourceLocks(), (MimeTyper) mockMimeTyper.proxy(), 0);

        mockReq.expects(once()).method("getAttribute").with(
                eq("javax.servlet.include.request_uri"))
                .will(returnValue(null));

        mockReq.expects(once()).method("getMethod").withNoArguments();
        
        mockReq.expects(once()).method("getPathInfo").withNoArguments().will(
                returnValue("/index.html"));
        mockReq.expects(once()).method("getRequestURI").withNoArguments().will(
                returnValue("/index.html"));

        mockRes.expects(once()).method("sendError").with(eq(404),
                eq("/index.html"));
        mockRes.expects(once()).method("setStatus").with(eq(404));
        doGet.execute((HttpServletRequest) mockReq.proxy(),
                (HttpServletResponse) mockRes.proxy());

    }


    public void testAccessOfaPageResultsInPage() throws Exception {

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

        mockMimeTyper.expects(once()).method("getMimeType").with(eq("/index.html")).will(returnValue("text/foo"));

        DoGet doGet = new DoGet((WebdavStore) mockStore.proxy(), null, null,
                new ResourceLocks(), (MimeTyper) mockMimeTyper.proxy(),0);

        mockReq.expects(once()).method("getAttribute").with(
                eq("javax.servlet.include.request_uri"))
                .will(returnValue(null));

        mockReq.expects(once()).method("getPathInfo").withNoArguments().will(
                returnValue("/index.html"));
        mockReq.expects(once()).method("getMethod").withNoArguments();
        mockRes.expects(once()).method("setDateHeader").with(eq("last-modified"), eq(now.getTime()));
        mockRes.expects(once()).method("setContentType").with(eq("text/foo"));
        TestingOutputStream tos = new TestingOutputStream();
        mockRes.expects(once()).method("getOutputStream").withNoArguments().will(returnValue(tos));

        doGet.execute((HttpServletRequest) mockReq.proxy(),
                (HttpServletResponse) mockRes.proxy());

        assertEquals("<hello/>", tos.toString());

    }


    public void testAccessOfaDirectoryResultsInRudimentaryChildList() throws Exception {

        mockStore.expects(exactly(2)).method("isFolder").with(
                eq("/foo/")).will(returnValue(true));
        mockStore.expects(once()).method("objectExists").with(
                eq("/foo/")).will(returnValue(false));
        mockStore.expects(once()).method("isResource").with(eq("/foo/"))
                .will(returnValue(false));
        mockStore.expects(once()).method("getChildrenNames").with(eq("/foo/"))
                .will(returnValue(new String[] {"AAA","BBB"}));

        DoGet doGet = new DoGet((WebdavStore) mockStore.proxy(), null, null,
                new ResourceLocks(), (MimeTyper) mockMimeTyper.proxy(), 0);

        mockReq.expects(once()).method("getAttribute").with(
                eq("javax.servlet.include.request_uri"))
                .will(returnValue(null));

        mockReq.expects(once()).method("getPathInfo").withNoArguments().will(
                returnValue("/foo/"));

        TestingOutputStream tos = new TestingOutputStream();
        mockRes.expects(once()).method("getOutputStream").withNoArguments().will(returnValue(tos));
        mockReq.expects(once()).method("getMethod").withNoArguments();
        
        doGet.execute((HttpServletRequest) mockReq.proxy(),
                (HttpServletResponse) mockRes.proxy());

        assertEquals("Contents of this Folder:\nAAA\nBBB\n", tos.toString());

    }

    public void testAccessOfaDirectoryResultsInRedirectIfDefaultIndexFilePresent() throws Exception {

        mockStore.expects(once()).method("isFolder").with(
                eq("/foo/")).will(returnValue(true));

        DoGet doGet = new DoGet((WebdavStore) mockStore.proxy(), "/yeehaaa", null,
                new ResourceLocks(), (MimeTyper) mockMimeTyper.proxy(), 0);

        mockReq.expects(once()).method("getAttribute").with(
                eq("javax.servlet.include.request_uri"))
                .will(returnValue(null));

        mockReq.expects(once()).method("getPathInfo").withNoArguments().will(
                returnValue("/foo/"));
        mockReq.expects(once()).method("getRequestURI").withNoArguments().will(
                returnValue("/foo"));

        mockRes.expects(once()).method("encodeRedirectURL").with(eq("/foo/yeehaaa"));
        mockRes.expects(once()).method("sendRedirect").with(eq(null));
        mockReq.expects(once()).method("getMethod").withNoArguments();

        doGet.execute((HttpServletRequest) mockReq.proxy(),
                (HttpServletResponse) mockRes.proxy());


    }

    public void testAccessOfaMissingPageResultsInPossibleAlternatveTo404() throws Exception {

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

        mockMimeTyper.expects(once()).method("getMimeType").with(eq("/yeehaaa")).will(returnValue("text/foo"));

        DoGet doGet = new DoGet((WebdavStore) mockStore.proxy(), null, "/yeehaaa",
                new ResourceLocks(), (MimeTyper) mockMimeTyper.proxy(), 0);
        
        mockReq.expects(once()).method("getMethod").withNoArguments();
        mockReq.expects(once()).method("getAttribute").with(
                eq("javax.servlet.include.request_uri"))
                .will(returnValue(null));

        mockReq.expects(once()).method("getPathInfo").withNoArguments().will(
                returnValue("/index.html"));

        mockRes.expects(once()).method("setDateHeader").with(eq("last-modified"), eq(now.getTime()));
        mockRes.expects(once()).method("setContentType").with(eq("text/foo"));
        TestingOutputStream tos = new TestingOutputStream();
        mockRes.expects(once()).method("getOutputStream").withNoArguments().will(returnValue(tos));
        mockRes.expects(once()).method("setStatus").with(eq(404));
        doGet.execute((HttpServletRequest) mockReq.proxy(), (HttpServletResponse) mockRes.proxy());

        assertEquals("<hello/>", tos.toString());

    }


}
