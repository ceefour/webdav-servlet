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
import java.io.StringReader;
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
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
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


	public static String format(String xml) {
		String retval = xml;
		
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		Document document = null;
		StringReader sr = null;
		try {
	    	sr = new StringReader(xml);
	    	InputSource inputSource = new InputSource(sr);
	        documentBuilderFactory.setNamespaceAware(true);
	        document = documentBuilderFactory.newDocumentBuilder().parse(inputSource);
		} catch (SAXException | IOException | ParserConfigurationException e) {
			e.printStackTrace();
		} finally {
			sr.close();
		}
	    if(document!=null) {
			retval = format(document);
	    }
	    return retval;
	}


	public static String format(Document document) {
		String retval = null;
        if(document!=null) {
    		TransformerFactory transfac = TransformerFactory.newInstance();
            StringWriter sw = null;
    		try {
    			Transformer transformer = transfac.newTransformer();
    			
    			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
    			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
				transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
    			transformer.setOutputProperty(OutputKeys.INDENT, "yes");

    			  //create string from xml tree
    	        sw = new StringWriter();
    	        StreamResult result = new StreamResult(sw);
    	        
    	        DOMSource source = new DOMSource(document);
    	        
    	        transformer.transform(source, result);
    	        
    	        retval = sw.toString();
    		} catch (TransformerException e) {
    			e.printStackTrace();
    		} finally {
    			try {
					sw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
    		}
        }
        return retval;
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
				String propertyName = null;
				if (nodeName.indexOf(':') != -1) {
					propertyName = nodeName.substring(nodeName.indexOf(':') + 1);
				} else {
					propertyName = nodeName;
				}
				// href is a live property which is handled differently
				properties.addElement(propertyName);
			}
		}
		return properties;
	}


}
