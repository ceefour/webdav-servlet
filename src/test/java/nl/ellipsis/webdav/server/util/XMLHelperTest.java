package nl.ellipsis.webdav.server.util;

import static org.junit.Assert.*;

import org.junit.Test;

import nl.ellipsis.webdav.server.util.XMLHelper;

public class XMLHelperTest {

	@Test
	public void testFormat() {
		String xml = "<root><sub><subsub></subsub></sub></root>";
		String expectedXml = "<root>\r\n  <sub>\r\n    <subsub/>\r\n  </sub>\r\n</root>\r\n";
		String formattedXml = XMLHelper.format(xml);
		assertEquals(expectedXml,formattedXml);
	}

}
