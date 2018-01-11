package net.sf.webdav.methods;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.webdav.IMethodExecutor;
import net.sf.webdav.ITransaction;
import net.sf.webdav.WebDAVStatus;

public class DoNotImplemented implements IMethodExecutor {

	private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DoNotImplemented.class);
	private boolean _readOnly;

	public DoNotImplemented(boolean readOnly) {
		_readOnly = readOnly;
	}

	public void execute(ITransaction transaction, HttpServletRequest req, HttpServletResponse resp) throws IOException {
		LOG.debug("-- " + req.getMethod());

		if (_readOnly) {
			resp.sendError(WebDAVStatus.SC_FORBIDDEN);
		} else {
			resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
		}
	}
}
