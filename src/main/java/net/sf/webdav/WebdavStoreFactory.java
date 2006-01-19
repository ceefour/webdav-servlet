package net.sf.webdav;

public class WebdavStoreFactory {

	private Class fImplementation;

	public WebdavStoreFactory(Class class1) {
		fImplementation = class1;
	}

	public IWebdavStorage getStore() throws InstantiationException, IllegalAccessException {
		return (IWebdavStorage) fImplementation.newInstance();
	}

}
