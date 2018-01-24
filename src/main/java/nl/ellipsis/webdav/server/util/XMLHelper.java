/*
 * Copyright 2018 Ellipsis BV, The Netherlands
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.ellipsis.webdav.server.util;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XMLHelper {
	
	private static DocumentBuilder documentBuilder;
	
	/**
	 * Return JAXP document builder instance.
	 * @throws ParserConfigurationException 
	 */
	public static DocumentBuilder getDocumentBuilder() throws ServletException, ParserConfigurationException {
		if(documentBuilder == null) {
			DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
			documentBuilderFactory.setNamespaceAware(true);
			documentBuilder = documentBuilderFactory.newDocumentBuilder();
		}
		return documentBuilder;
	}
	

	public static Node findSubElement(Node parent, String localName) {
		if (parent == null) {
			return null;
		}
		Node child = parent.getFirstChild();
		while (child != null) {
			if ((child.getNodeType() == Node.ELEMENT_NODE) && (child.getLocalName().equals(localName))) {
				return child;
			}
			child = child.getNextSibling();
		}
		return null;
	}

	public static Vector<String> getPropertiesFromXML(Node propNode) {
		Vector<String> properties;
		properties = new Vector<String>();
		NodeList childList = propNode.getChildNodes();

		for (int i = 0; i < childList.getLength(); i++) {
			Node currentNode = childList.item(i);
			if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
				String nodeName = currentNode.getLocalName();
				String namespace = currentNode.getNamespaceURI();
				// href is a live property which is handled differently
				properties.addElement(namespace + ":" + nodeName);
			}
		}
		return properties;
	}
	
	public static String format(String xml) {
		String retval = null;
		if(xml!=null) {
			try {
				DocumentBuilder documentBuilder = getDocumentBuilder();
				
				Transformer transformer = TransformerFactory.newInstance().newTransformer();
				
				transformer.setOutputProperty(OutputKeys.INDENT, "yes");
				transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
				transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
				
				StreamResult result = new StreamResult(new StringWriter());
				DOMSource source = new DOMSource(documentBuilder.parse(IOUtils.toInputStream(xml,java.nio.charset.StandardCharsets.UTF_8.name())));
				transformer.transform(source, result);
				
	    		retval = result.getWriter().toString();
			} catch (ServletException | ParserConfigurationException | TransformerFactoryConfigurationError | SAXException | TransformerException | IOException e) {
				e.printStackTrace();
				retval = xml;
			}
		}
		return retval;
	}

}
