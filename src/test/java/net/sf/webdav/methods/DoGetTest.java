package net.sf.webdav.methods;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.webdav.ResourceLocks;
import net.sf.webdav.WebdavStore;

import org.jmock.Mock;
import org.jmock.MockObjectTestCase;

public class DoGetTest extends MockObjectTestCase {

    public void testDoGet() throws Exception {

        Mock mockWebdav = mock(WebdavStore.class);
        mockWebdav.expects(exactly(2)).method("isFolder").with(
                eq("/index.html")).will(returnValue(false));
        mockWebdav.expects(exactly(2)).method("objectExists").with(
                eq("/index.html")).will(returnValue(false));
        mockWebdav.expects(once()).method("isResource").with(eq("/index.html"))
                .will(returnValue(false));

        DoGet doGet = new DoGet((WebdavStore) mockWebdav.proxy(), null, null,
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
}
