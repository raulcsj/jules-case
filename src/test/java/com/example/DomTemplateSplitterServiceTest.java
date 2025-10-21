package com.example;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DomTemplateSplitterServiceTest {

    @Test
    void testSplitAndCompare() throws Exception {
        // Run the splitter service
        String inputFile = "zbx_export_templates.xml";
        String outputDirectory = "test-output";
        DomTemplateSplitterService splitterService = new DomTemplateSplitterService();
        splitterService.split(inputFile, outputDirectory);

        // Compare the generated file with the original
        Path generatedFilePath = Path.of(outputDirectory, "Acronis_Cyber_Protect_Cloud_by_HTTP.xml");
        Path originalFilePath = Path.of("zbx_export_templates (1).xml");

        String generatedContent = getNormalizedXml(generatedFilePath);
        String originalContent = getNormalizedXml(originalFilePath);

        assertEquals(originalContent, generatedContent);

        // Clean up the output directory
        Files.delete(generatedFilePath);
        Files.delete(Path.of(outputDirectory, "Acronis_Cyber_Protect_Cloud_MSP_by_HTTP.xml"));
        Files.delete(Path.of(outputDirectory, "AIX_by_Zabbix_agent.xml"));
        Files.delete(Path.of(outputDirectory));
    }

    private String getNormalizedXml(Path path) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(path.toFile());
        doc.getDocumentElement().normalize();

        // Remove the date element for comparison
        NodeList dateNodes = doc.getElementsByTagName("date");
        if (dateNodes.getLength() > 0) {
            Node dateNode = dateNodes.item(0);
            dateNode.getParentNode().removeChild(dateNode);
        }

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));

        return writer.toString()
                     .replaceAll("\\r\\n", "\n")
                     .replaceAll(">\\s+<", "><")
                     .trim();
    }
}
