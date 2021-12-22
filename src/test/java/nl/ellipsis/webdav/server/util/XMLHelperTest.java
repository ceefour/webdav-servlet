package nl.ellipsis.webdav.server.util;

import static org.junit.Assert.*;

import org.hamcrest.CoreMatchers;
import org.hamcrest.core.StringContains;
import org.junit.Assert;
import org.junit.Test;

import nl.ellipsis.webdav.server.util.XMLHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class XMLHelperTest {

	@Test
	public void testFormat() {
		String lineSeparator = System.getProperty("line.separator");

		String xml = "<root><sub><subsub></subsub></sub></root>";
		String expectedXml = "<root>"+lineSeparator+"  <sub>"+lineSeparator+"    <subsub/>"+lineSeparator+"  </sub>"+lineSeparator+"</root>"+lineSeparator;
		String formattedXml = XMLHelper.format(xml);

		assertEquals(expectedXml, formattedXml);
	}

	@Test
	public void testXXEDefense() throws IOException {
		Path testExternalPath = Files.createTempFile(XMLHelperTest.class.getName(), "test");
		try {
			Files.write(testExternalPath, "xyzzy".getBytes());

			String xml = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n"
				+ " <!DOCTYPE foo [\n"
				+ " <!ELEMENT foo ANY >\n"
				+ " <!ENTITY xxe SYSTEM \"file://" + testExternalPath.toAbsolutePath().toString() + "\" >]>\n"
				+ " <foo>&xxe;</foo>";

			String formattedXml = XMLHelper.format(xml);

			Assert.assertThat(formattedXml, CoreMatchers.not(StringContains.containsString("xyzzy")));
		} finally {
			Files.delete(testExternalPath);
		}
	}

}
