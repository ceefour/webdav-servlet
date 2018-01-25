package nl.ellipsis.webdav.server;

public interface WebDAVConstants {
	public final static String PFX_ACL_PFXDOCUMENT = "PfxDocuments";
	public final static String PREFIX_DOMAIN_PERMISSION = "imDomain";
	public final static String ENCODING_UTF8 = "UTF-8";
	public final static String CONTENTTYPE_XML_UTF8 = "text/xml; charset=utf-8";
	// public static final String RESOURCES_ATTR = "nl.ellipsis.webdav.naming.resources";

	public interface HttpHeader {
		public final static String ALLOW			= "Allow";
		public final static String CONTENT_LENGTH	= "content-length"; 
		public final static String DAV				= "DAV";
		public final static String DEPTH			= "Depth";
		public final static String DESTINATION		= "Destination";
		public final static String ETAG				= "ETag";
		public final static String IF				= "If";
		public final static String IF_NONE_MATCH	= "If-None-Match";
		public final static String LOCK_TOKEN		= "Lock-Token";
		public final static String MS_AUTHOR_VIA	= "MS-Author-Via";
		public final static String OVERWRITE		= "Overwrite";
		public final static String TIMEOUT			= "Timeout";
		public final static String USER_AGENT 		= "User-Agent"; 
	}

	public interface HttpRequestParam {
	    public static final String INCLUDE_CONTEXT_PATH 		= "javax.servlet.include.context_path";
	    public static final String INCLUDE_PATH_INFO 			= "javax.servlet.include.path_info";
	    public static final String INCLUDE_REQUEST_URI 			= "javax.servlet.include.request_uri";
	    public static final String INCLUDE_SERVLET_PATH 		= "javax.servlet.include.servlet_path";
	}
	
	public interface Permission {
		public final static String PFX_DOCUMENT_METHOD_COPY				= "pfxDocumentMethodCopy";
		public final static String PFX_DOCUMENT_METHOD_DELETE			= "pfxDocumentMethodDelete";
		public final static String PFX_DOCUMENT_METHOD_GET				= "pfxDocumentMethodGet";
		public final static String PFX_DOCUMENT_METHOD_LOCK				= "pfxDocumentMethodLock";
		public final static String PFX_DOCUMENT_METHOD_MKCOL			= "pfxDocumentMethodMkcol";
		public final static String PFX_DOCUMENT_METHOD_MOVE				= "pfxDocumentMethodMove";
		public final static String PFX_DOCUMENT_METHOD_PROPFIND 		= "pfxDocumentMethodProppatch";
		public final static String PFX_DOCUMENT_METHOD_PROPPATCH 		= "pfxDocumentMethodPropfind";
		public final static String PFX_DOCUMENT_METHOD_PUT				= "pfxDocumentMethodPut";
		public final static String PFX_DOCUMENT_METHOD_UNLOCK 			= "pfxDocumentMethodUnlock";
		
		public final static String PFX_ACCESS_WEBDAV_ROOTDIR			= "pfxAccessWebDAVRootDir";
		public final static String PFX_ACCESS_WEBDAV_DOMAINDIR			= "pfxAccessWebDAVDomainDir";
		public final static String PFX_ACCESS_WEBDAV_DOMAINDIR_SUB1		= "pfxAccessWebDAVDomainDirSub1";
		public final static String PFX_ACCESS_WEBDAV_DOMAINDIR_SUB2 	= "pfxAccessWebDAVDomainDirSub2";
		public final static String PFX_ACCESS_WEBDAV_DIRECTORY_LISTING	= "pfxAccessWebDAVDirectoryListing";
		public final static String PFX_ACCESS_WEBDAV_METHOD_COPY 		= "pfxAccessWebDAVMethodCopy";
		public final static String PFX_ACCESS_WEBDAV_METHOD_DELETE 		= "pfxAccessWebDAVMethodDelete";
		public final static String PFX_ACCESS_WEBDAV_METHOD_GET 		= "pfxAccessWebDAVMethodGet";
		public final static String PFX_ACCESS_WEBDAV_METHOD_LOCK 		= "pfxAccessWebDAVMethodLock";
		public final static String PFX_ACCESS_WEBDAV_METHOD_MKCOL 		= "pfxAccessWebDAVMethodMkcol";
		public final static String PFX_ACCESS_WEBDAV_METHOD_MOVE 		= "pfxAccessWebDAVMethodMove";
		public final static String PFX_ACCESS_WEBDAV_METHOD_PROPFIND 	= "pfxAccessWebDAVMethodProppatch";
		public final static String PFX_ACCESS_WEBDAV_METHOD_PROPPATCH 	= "pfxAccessWebDAVMethodPropfind";
		public final static String PFX_ACCESS_WEBDAV_METHOD_PUT 		= "pfxAccessWebDAVMethodPut";
		public final static String PFX_ACCESS_WEBDAV_METHOD_UNLOCK 		= "pfxAccessWebDAVMethodUnlock";
	}
	
	public interface XMLTag {
		public final static String ACTIVELOCK				= "activelock";
		public static final String ALLPROP	 				= "allprop";	
		public final static String COLLECTION				= "collection";
		public final static String CREATIONDATE				= "creationdate";
		public final static String DEPTH					= "depth";
		public final static String DISPLAYNAME				= "displayname";
		public static final String EXCLUSIVE 				= "exclusive";
		public final static String GET_CONTENTLANGUAGE		= "getcontentlanguage";
		public final static String GET_CONTENTLENGTH		= "getcontentlength";
		public final static String GET_CONTENTTYPE			= "getcontenttype";
		public final static String GET_ETAG					= "getetag";
		public final static String GET_LASTMODIFIED			= "getlastmodified";
		public final static String HREF						= "href";
		public final static String LOCK_NULL				= "lock-null";
		public final static String LOCKDISCOVERY			= "lockdiscovery";
		public static final String LOCKENTRY 				= "lockentry";
		public final static String LOCKSCOPE				= "lockscope";
		public final static String LOCKTOKEN				= "locktoken";
		public final static String LOCKTYPE					= "locktype";
		public final static String OWNER					= "owner";
		public final static String PROP						= "prop";
		public static final String PROPERTYUPDATE 			= "propertyupdate";	
		public static final String PROPNAME 				= "propname";	
		public final static String PROPSTAT					= "propstat";
		public final static String MULTISTATUS				= "multistatus";
		public final static String RESPONSE					= "response";
		public final static String RESOURCETYPE				= "resourcetype";
		public final static String SOURCE					= "source";
		public static final String SHARED 					= "shared";
		public final static String STATUS					= "status";
		public final static String SUPPORTEDLOCK			= "supportedlock";
		public final static String TIMEOUT					= "timeout";
		public static final String WRITE 					= "write";
	}
	
}
