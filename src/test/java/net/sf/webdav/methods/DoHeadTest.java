package net.sf.webdav.methods;

import org.jmock.MockObjectTestCase;
import org.jmock.Mock;
import net.sf.webdav.WebdavStore;
import net.sf.webdav.MimeTyper;
import net.sf.webdav.ResourceLocks;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.io.ByteArrayInputStream;

public class DoHeadTest extends MockObjectTestCase {

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

        mockStore.expects(once()).method("isFolder").with(
                eq("/index.html")).will(returnValue(false));
        mockStore.expects(once()).method("objectExists").with(
                eq("/index.html")).will(returnValue(false));
        mockStore.expects(once()).method("isResource").with(eq("/index.html"))
                .will(returnValue(false));

        DoHead doHead = new DoHead((WebdavStore) mockStore.proxy(), null, null,
                new ResourceLocks(), (MimeTyper) mockMimeTyper.proxy(), 0, 0);

        mockReq.expects(once()).method("getAttribute").with(
                eq("javax.servlet.include.request_uri"))
                .will(returnValue(null));

        mockReq.expects(once()).method("getPathInfo").withNoArguments().will(
                returnValue("/index.html"));

        doHead.execute((HttpServletRequest) mockReq.proxy(),
                (HttpServletResponse) mockRes.proxy());

        // TODO should this return something to the browser ?

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

        mockMimeTyper.expects(once()).method("getMimeType").with(eq("/index.html")).will(returnValue("text/foo"));

        DoHead doHead = new DoHead((WebdavStore) mockStore.proxy(), null, null,
                new ResourceLocks(), (MimeTyper) mockMimeTyper.proxy(),0, 0);

        mockReq.expects(once()).method("getAttribute").with(
                eq("javax.servlet.include.request_uri"))
                .will(returnValue(null));

        mockReq.expects(once()).method("getPathInfo").withNoArguments().will(
                returnValue("/index.html"));

        mockRes.expects(once()).method("setDateHeader").with(eq("last-modified"), eq(now.getTime()));
        mockRes.expects(once()).method("setContentType").with(eq("text/foo"));

        doHead.execute((HttpServletRequest) mockReq.proxy(),
                (HttpServletResponse) mockRes.proxy());

    }


    public void testAccessOfaDirectoryResultsInRedirectIfDefaultIndexFilePresent() throws Exception {

        mockStore.expects(once()).method("isFolder").with(
                eq("/foo/")).will(returnValue(true));

        DoHead doHead = new DoHead((WebdavStore) mockStore.proxy(), "/yeehaaa", null,
                new ResourceLocks(), (MimeTyper) mockMimeTyper.proxy(), 0, 0);

        mockReq.expects(once()).method("getAttribute").with(
                eq("javax.servlet.include.request_uri"))
                .will(returnValue(null));

        mockReq.expects(once()).method("getPathInfo").withNoArguments().will(
                returnValue("/foo/"));
        mockReq.expects(once()).method("getRequestURI").withNoArguments().will(
                returnValue("/foo"));

        mockRes.expects(once()).method("encodeRedirectURL").with(eq("/foo/yeehaaa"));
        mockRes.expects(once()).method("sendRedirect").with(eq(null));

        doHead.execute((HttpServletRequest) mockReq.proxy(),
                (HttpServletResponse) mockRes.proxy());


    }

}
