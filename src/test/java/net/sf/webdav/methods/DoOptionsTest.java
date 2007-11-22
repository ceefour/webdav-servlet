package net.sf.webdav.methods;

import org.jmock.Mock;
import org.jmock.MockObjectTestCase;
import net.sf.webdav.WebdavStore;
import net.sf.webdav.MimeTyper;
import net.sf.webdav.ResourceLocks;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class DoOptionsTest extends MockObjectTestCase {

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

    public void testOptionsOnExistingNode() throws IOException {

        mockReq.expects(once()).method("getAttribute").with(
                eq("javax.servlet.include.request_uri"))
                .will(returnValue(null));
        mockReq.expects(once()).method("getPathInfo").withNoArguments().will(
                returnValue("/index.html"));

        mockRes.expects(once()).method("addHeader").with(eq("DAV"), eq("1, 2"));

        mockStore.expects(once()).method("objectExists").with(
                eq("/index.html")).will(returnValue(true));

        mockStore.expects(once()).method("isFolder").with(
                eq("/index.html")).will(returnValue(false));
        
        mockRes.expects(once()).method("addHeader").with(eq("Allow"), eq("OPTIONS, GET, HEAD, POST, DELETE, TRACE, PROPPATCH, COPY, MOVE, LOCK, UNLOCK, PROPFIND"));

        mockRes.expects(once()).method("addHeader").with(eq("MS-Author-Via"), eq("DAV"));

        DoOptions doOptions = new DoOptions((WebdavStore) mockStore.proxy(), new ResourceLocks());
        doOptions.execute((HttpServletRequest) mockReq.proxy(), (HttpServletResponse) mockRes.proxy());
    }

    public void testOptionsOnNonExistingNode() throws IOException {

        mockReq.expects(once()).method("getAttribute").with(
                eq("javax.servlet.include.request_uri"))
                .will(returnValue(null));
        mockReq.expects(once()).method("getPathInfo").withNoArguments().will(
                returnValue("/index.html"));

        mockRes.expects(once()).method("addHeader").with(eq("DAV"), eq("1, 2"));

        mockStore.expects(once()).method("objectExists").with(
                eq("/index.html")).will(returnValue(false));

        mockStore.expects(once()).method("isFolder").with(
                eq("/index.html")).will(returnValue(false));

        mockRes.expects(once()).method("addHeader").with(eq("Allow"), eq("OPTIONS, MKCOL, PUT, LOCK"));

        mockRes.expects(once()).method("addHeader").with(eq("MS-Author-Via"), eq("DAV"));

        DoOptions doOptions = new DoOptions((WebdavStore) mockStore.proxy(), new ResourceLocks());
        doOptions.execute((HttpServletRequest) mockReq.proxy(), (HttpServletResponse) mockRes.proxy());
    }


}
