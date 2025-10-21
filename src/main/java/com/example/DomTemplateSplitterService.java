package com.example;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DomTemplateSplitterService {

    public void split(String inputFile, String outputDir) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new File(inputFile));
        doc.getDocumentElement().normalize();

        NodeList templatesNodes = doc.getElementsByTagName("templates");
        if (templatesNodes.getLength() == 0) {
            return; // No templates found
        }
        Element templatesContainer = (Element) templatesNodes.item(0);
        NodeList templateNodes = templatesContainer.getChildNodes();

        for (int i = 0; i < templateNodes.getLength(); i++) {
            Node templateNode = templateNodes.item(i);
            if (templateNode.getNodeType() == Node.ELEMENT_NODE && "template".equals(templateNode.getNodeName())) {
                Element templateElement = (Element) templateNode;
                String templateName = getFirstElementValueByTagName(templateElement, "name");

                if (templateName == null) {
                    continue; // Skip templates without a name
                }

                Document newDoc = builder.newDocument();
                Element root = newDoc.createElement("zabbix_export");
                newDoc.appendChild(root);

                // Copy version and date
                root.appendChild(newDoc.importNode(doc.getElementsByTagName("version").item(0), true));
                root.appendChild(newDoc.importNode(doc.getElementsByTagName("date").item(0), true));

                // Add groups
                Element groupsElement = newDoc.createElement("groups");
                List<String> templateGroupNames = getElementValuesByTagName(templateElement, "group", "name");

                NodeList topLevelGroups = doc.getElementsByTagName("groups");
                if (topLevelGroups.getLength() > 0) {
                    NodeList definedGroups = ((Element) topLevelGroups.item(0)).getChildNodes();
                    for (int j = 0; j < definedGroups.getLength(); j++) {
                        Node groupNode = definedGroups.item(j);
                        if (groupNode.getNodeType() == Node.ELEMENT_NODE && "group".equals(groupNode.getNodeName())) {
                            Element groupElement = (Element) groupNode;
                            String groupName = getFirstElementValueByTagName(groupElement, "name");
                            if (groupName != null && templateGroupNames.contains(groupName)) {
                                groupsElement.appendChild(newDoc.importNode(groupNode, true));
                            }
                        }
                    }
                }
                root.appendChild(groupsElement);

                // Add templates section
                Element newTemplatesElement = newDoc.createElement("templates");
                newTemplatesElement.appendChild(newDoc.importNode(templateElement, true));
                root.appendChild(newTemplatesElement);

                // Add related graphs
                NodeList graphsNodeList = doc.getElementsByTagName("graphs");
                if (graphsNodeList.getLength() > 0) {
                    Element newGraphsElement = newDoc.createElement("graphs");
                    boolean hasGraphs = false;
                    NodeList allGraphs = ((Element) graphsNodeList.item(0)).getElementsByTagName("graph");
                    for (int j = 0; j < allGraphs.getLength(); j++) {
                        Node graphNode = allGraphs.item(j);
                        if (isGraphRelatedToTemplate((Element) graphNode, templateName)) {
                            newGraphsElement.appendChild(newDoc.importNode(graphNode, true));
                            hasGraphs = true;
                        }
                    }
                    if (hasGraphs) {
                        root.appendChild(newGraphsElement);
                    }
                }

                // Add related value maps
                NodeList valueMapsNodeList = doc.getElementsByTagName("valuemaps");
                if (valueMapsNodeList.getLength() > 0) {
                    Element newValueMapsElement = newDoc.createElement("valuemaps");
                    boolean hasValueMaps = false;
                    List<String> templateValueMapNames = getElementValuesByTagName(templateElement, "valuemap", "name");
                    NodeList allValueMaps = ((Element) valueMapsNodeList.item(0)).getElementsByTagName("valuemap");
                    for (int j = 0; j < allValueMaps.getLength(); j++) {
                        Node valueMapNode = allValueMaps.item(j);
                        Element valueMapElement = (Element) valueMapNode;
                        String valueMapName = getFirstElementValueByTagName(valueMapElement, "name");
                        if (valueMapName != null && templateValueMapNames.contains(valueMapName)) {
                            newValueMapsElement.appendChild(newDoc.importNode(valueMapNode, true));
                            hasValueMaps = true;
                        }
                    }
                    if (hasValueMaps) {
                        root.appendChild(newValueMapsElement);
                    }
                }

                String outputFileName = templateName.replaceAll("[^a-zA-Z0-9.-]", "_") + ".xml";
                File outputFile = new File(outputDir, outputFileName);
                outputFile.getParentFile().mkdirs();
                writeXml(newDoc, outputFile);
            }
        }
    }

    private String getFirstElementValueByTagName(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            if(nodes.item(0).getChildNodes().getLength() > 0) {
                return nodes.item(0).getTextContent();
            }
        }
        return null;
    }

    private List<String> getElementValuesByTagName(Element parent, String tagName, String childTagName) {
        List<String> values = new ArrayList<>();
        NodeList nodes = parent.getElementsByTagName(tagName);
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                String value = getFirstElementValueByTagName((Element) node, childTagName);
                if (value != null) {
                    values.add(value);
                }
            }
        }
        return values;
    }

    private boolean isGraphRelatedToTemplate(Element graphElement, String templateName) {
        if (templateName == null) {
            return false;
        }
        NodeList graphItems = graphElement.getElementsByTagName("item");
        for (int i = 0; i < graphItems.getLength(); i++) {
            Element itemElement = (Element) graphItems.item(i);
            String host = getFirstElementValueByTagName(itemElement, "host");
            if (templateName.equals(host)) {
                return true;
            }
        }
        return false;
    }

    private void writeXml(Document doc, File file) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(file);
        transformer.transform(source, result);
    }
}
