package net.sf.webdav;

import java.util.Enumeration;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Mock;
import org.jmock.MockObjectTestCase;

public class WebdavServletTest extends MockObjectTestCase {

    
    public void testname() throws Exception {
    assertTrue(true);
    }
//	private WebDavServletBean _servlet;
//
//	protected void setUp() throws Exception {
//		_servlet = new WebdavServlet();
//		Mock servletConfigMock = mock(ServletConfig.class);
//
//		// we should have a resource Handler Factory
//		servletConfigMock.stubs().method("getInitParameter").with(
//				eq("ResourceHandlerImplementation")).will(
//				returnValue("net.sf.webdav.LocalFileSystemStorage"));
//
//		servletConfigMock.stubs().method("getInitParameter").with(
//				eq("rootpath")).will(returnValue("./target/tmpTestData/"));
//
//		// init parameter names
//		Mock enumarationMock = mock(Enumeration.class);
//		enumarationMock.stubs().method("hasMoreElements").withNoArguments()
//				.will(returnValue(false));
//		// the very first time we return true
//		enumarationMock.expects(exactly(1)).method("hasMoreElements")
//				.withNoArguments().will(returnValue(true));
//
//		enumarationMock.stubs().method("nextElement").withNoArguments().will(
//				returnValue("rootpath"));
//
//		Enumeration proxy = (Enumeration) enumarationMock.proxy();
//		servletConfigMock.stubs().method("getInitParameterNames")
//				.withAnyArguments().will(returnValue(proxy));
//
//		ServletConfig servletConfig = (ServletConfig) servletConfigMock.proxy();
//		_servlet.init(servletConfig);
//	}
//
//	public void testPropFind() throws Exception {
//		Mock requestMock = mock(HttpServletRequest.class);
//		Mock responseMock = mock(HttpServletResponse.class);
//		_servlet.doPut((HttpServletRequest) requestMock.proxy(),
//				(HttpServletResponse) responseMock.proxy());
//	}
//
//	public void testPropPatch() throws Exception {
//	}
//
//	public void testMkCol() throws Exception {
//
//	}
//
//	public void testGet() throws Exception {
//
//	}
//
//	public void testHead() throws Exception {
//
//	}
//
//	public void testPost() throws Exception {
//
//	}
//
//	public void testDelete() throws Exception {
//
//	}
//
//	public void testPut() throws Exception {
//
//	}
//
//	public void testCopy() throws Exception {
//
//	}
//
//	public void testMove() throws Exception {
//
//	}
//
//	public void testLock() throws Exception {
//
//	}
//
//	public void testUnlock() throws Exception {
//
//	}

}
