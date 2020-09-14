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

		assertEquals(expectedXml, formattedXml);
	}

}
