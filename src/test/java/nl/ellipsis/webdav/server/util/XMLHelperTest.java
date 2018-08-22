package nl.ellipsis.webdav.server.util;

import static org.junit.Assert.*;

import org.junit.Test;

import nl.ellipsis.webdav.server.util.XMLHelper;

public class XMLHelperTest {

	@Test
	public void testFormat() {
		String lineSeparator = System.getProperty("line.separator");

		String xml = "<root><sub><subsub></subsub></sub></root>";
		String expectedXml = "<root>"+lineSeparator+"  <sub>"+lineSeparator+"    <subsub/>"+lineSeparator+"  </sub>"+lineSeparator+"</root>"+lineSeparator;
		String formattedXml = XMLHelper.format(xml);

		// XMLHelper appears to permit an XML declaration if the
		// underlying transformer chooses to produce one. The test
		// aims not to care either way.
		assertTrue(formattedXml.endsWith(expectedXml));
	}

}
