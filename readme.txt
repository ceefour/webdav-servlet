WebDAV-Servlet
===============

What is it?
  A Servlet that brings basic WebDAV access to any store. Only 1 interface 
  (IWebdavStorage) has to be implemented, an example (LocalFileSystemStorage)
  which uses the local filesystem, is provided.
  Unlike large systems (like slide), this servlet only supports the most basic
  data access options. locking, versioning or user management are not supported

  
REQUIREMENTS

  JDK 1.42 or above
  apache-tomcat 5.0.28 or above

INSTALLATION & CONFIGURATION

  -place the webdav-servlet.jar in the /WEB-INF/lib/ of your webapp
  -open web.xml of the webapp. it needs to contain the following:
  
  	<servlet>
		<servlet-name>webdav</servlet-name>
		<servlet-class>
			net.sf.webdav.WebdavServlet
		</servlet-class>
		<init-param>
			<param-name>ResourceHandlerImplementation</param-name>
			<param-value>
				net.sf.webdav.LocalFileSystemStorage
			</param-value>
			<description>
				name of the class that implements
				net.sf.webdav.IWebdavStorage
			</description>
		</init-param>
		<init-param>
			<param-name>rootpath</param-name>
			<param-value>d:/tmp/</param-value>
			<description>
				place where to store the webdavcontent on the local
				filesystem
			</description>
		</init-param>
		<init-param>
			<param-name>storeDebug</param-name>
			<param-value>0</param-value>
			<description>
				triggers debug output of the
				ResourceHandlerImplementation (0 = off , 1 = on) off by default
			</description>
		</init-param>
	</servlet>
	<servlet-mapping>
		<servlet-name>webdav</servlet-name>
		<url-pattern>/*</url-pattern>
	</servlet-mapping>
  
  -if you want to use the reference implementation, set the parameter "rootpath"
   to where you want to store your files
  -if you have implemented your own store, insert the class name
   to the parameter  "ResourceHandlerImplementation"
   and copy your .jar to /WEB-INF/lib/
  -with /* as servlet mapping, every request to the webapp is handled by
   the servlet. change this if you want
  -with the "storeDebug" parameter you can trigger the reference store implementation
   to spam at every method call. this parameter is optional and can be omitted
  -authentication is done by the servlet-container. If you need it, you have to
   add the appropriate sections to the web.xml


ACCESSING THE FILESTORE

  the webdav-filestore is reached at:
  "http://<ip/name + port of the server>/<name of the webapp>/<servlet-maping>"
                             e.g.:   http://localhost:8080/webdav-servlet

weta-dfs-webdav has been tested on tomcat 5.0.28 and 5.5.12

so far, we accessed it from windows(2000 and XP) and MAC


CREDITS

We want to thank Remy Maucherat for the original webdav-servlet
and the dependent files that come with tomcat,
and Oliver Zeigermann for the slide-WCK. Our IWebdavStorage class is modeled
after his BasicWebdavStore.
 
 
 
Thanks for trying WebDAV-Servlet!  

the project homepage is at:
<http://sourceforge.net/projects/webdav-servlet/>

sponsored by media style
<http://www.media-style.com>