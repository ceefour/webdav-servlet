package net.sf.webdav.fromcatalina;

import java.util.Vector;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XMLHelper {

    public static Node findSubElement(Node parent, String localName) {
        if (parent == null) {
            return null;
        }
        Node child = parent.getFirstChild();
        while (child != null) {
            if (child.getNodeType() == Node.ELEMENT_NODE
                    && child.getLocalName().equals(localName)) {
                return child;
            }
            child = child.getNextSibling();
        }
        return null;
    }

    public static Vector<String> getPropertiesFromXML(Node propNode) {
        Vector<String> properties = new Vector<>();
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

}
