package nl.ellipsis.webdav.server.util;

import static org.junit.Assert.*;

import org.junit.Test;

import nl.ellipsis.webdav.server.util.URLUtil;

public class URLUtilTest {

	@Test
	public void testGetCleanPath() {
		assertEquals(null,URLUtil.getCleanPath(null));
		assertEquals("/",URLUtil.getCleanPath(""));
		assertEquals("/",URLUtil.getCleanPath("/"));
		assertEquals("/file",URLUtil.getCleanPath("file"));
		assertEquals("/file",URLUtil.getCleanPath("/file"));
		assertEquals("/folder/file",URLUtil.getCleanPath("folder/file"));
		assertEquals("/folder/file",URLUtil.getCleanPath("/folder/file"));
		assertEquals("/folder/folder",URLUtil.getCleanPath("/folder/folder/"));
	}

	@Test
	public void testGetCleanPathConcat() {
		assertEquals(null,URLUtil.getCleanPath(null,null));
		assertEquals("/",URLUtil.getCleanPath("",""));
		assertEquals("/",URLUtil.getCleanPath("/",""));
		assertEquals("/",URLUtil.getCleanPath("","/"));
		assertEquals("/",URLUtil.getCleanPath("/","/"));
		assertEquals("/file",URLUtil.getCleanPath("/","file"));
		assertEquals("/file",URLUtil.getCleanPath("/","/file"));
		assertEquals("/file",URLUtil.getCleanPath("","/file"));
		assertEquals("/file",URLUtil.getCleanPath("","file"));
		assertEquals("/folder/file",URLUtil.getCleanPath("/","folder/file"));
		assertEquals("/folder/file",URLUtil.getCleanPath("/","/folder/file"));
		assertEquals("/folder/file",URLUtil.getCleanPath("","folder/file"));
		assertEquals("/folder/folder",URLUtil.getCleanPath("/","/folder/folder/"));
		assertEquals("/folder/file",URLUtil.getCleanPath(null,"folder/file"));
	}

	@Test
	public void testGetParentPath() {
		assertEquals(null,URLUtil.getParentPath(null));
		assertEquals(null,URLUtil.getParentPath("/"));
		assertEquals("/",URLUtil.getParentPath("file"));
		assertEquals("/",URLUtil.getParentPath("/file"));
		assertEquals("/",URLUtil.getParentPath("/folder/"));
		assertEquals("/",URLUtil.getParentPath("folder/"));
		assertEquals("/folder",URLUtil.getParentPath("/folder/file"));
		assertEquals("/folder",URLUtil.getParentPath("/folder/folder/"));
	}

	@Test
	public void testGetRelativePath() {
		assertEquals("/",URLUtil.getRelativePath(null));
		assertEquals("/",URLUtil.getRelativePath(""));
		assertEquals("/",URLUtil.getRelativePath("/"));
		assertEquals("/file",URLUtil.getRelativePath("file"));
		assertEquals("/file",URLUtil.getRelativePath("/file"));
		assertEquals("/folder/file",URLUtil.getRelativePath("folder/file"));
		assertEquals("/folder/file",URLUtil.getRelativePath("/folder/file"));
		assertEquals("/folder/folder",URLUtil.getRelativePath("/folder/folder/"));
	}

}
