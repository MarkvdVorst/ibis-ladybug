package nl.nn.testtool.transform;

import nl.nn.testtool.TestTool;
import org.apache.xalan.trace.TemplateTrace;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XslTransformerReporter {
    private final TestTool testTool;
    private final StreamSource xmlSource;
    private final StreamSource xslSource;
    private final String xsltResult;
    private final List<TemplateTrace> templateTraceList;

    public XslTransformerReporter(TestTool testTool, StreamSource xmlSource, StreamSource xslSource, List<TemplateTrace> templateTraceStack, String xsltResult) {
        //TODO: gebruik files
        this.testTool = testTool;
        this.xmlSource = xmlSource;
        this.xslSource = xslSource;
        this.templateTraceList = templateTraceStack;
        this.xsltResult = xsltResult;
    }

    public void Start(String correlationId, String reportName) {
        testTool.startpoint(correlationId, xmlSource.getPublicId(), "XSLT start point", "XSLT start point");
        try {
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer();
            transformer.transform(xmlSource, result);
            testTool.infopoint(correlationId, null, "XML Source", writer.toString());

            writer = new StringWriter();
            result.setWriter(writer);
            transformer.transform(xslSource, result);
            testTool.infopoint(correlationId, null, "XSL Source", writer.toString());

            PrintAllImportedXSL(correlationId);

            PrintCompleteTraceFromStack(correlationId);

            PrintCompleteXSLT(correlationId);

            LoopThroughAllTemplates(correlationId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        testTool.endpoint(correlationId, null, "XSLT end point", "XSLT end point");
    }

    private void PrintAllImportedXSL(String correlationId) {
        try {
            File file = new File(xslSource.getSystemId());
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document doc = documentBuilder.parse(file);
            doc.getDocumentElement().normalize();
            NodeList nodeList = doc.getElementsByTagName("xsl:import");
            if (nodeList.getLength() > 0) {
                testTool.startpoint(correlationId, null, "Imported XSL", "Imported XSL");
                for (int i = 0; i < nodeList.getLength(); i++) {
                    Element element = (Element) nodeList.item(i);
                    String importPath = element.getAttribute("href");
                    Path path = Paths.get(importPath);
                    String fileName = path.getFileName().toString();

                    StringWriter writer = new StringWriter();
                    StreamResult result = new StreamResult(writer);
                    TransformerFactory tFactory = TransformerFactory.newInstance();
                    Transformer transformer = tFactory.newTransformer();
                    transformer.transform(new StreamSource(path.toString()), result);

                    testTool.infopoint(correlationId, null, fileName, writer.toString());
                    writer = new StringWriter();
                    result.setWriter(writer);
                }
                testTool.endpoint(correlationId, null, "Imported XSL", "Imported XSL");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void PrintCompleteXSLT(String correlationId) {
        testTool.infopoint(correlationId, null, "Complete XSLT", xsltResult);
    }

    private void PrintCompleteTraceFromStack(String correlationId){
        StringBuilder result = new StringBuilder();

        //TODO: fix de volgorde en looks van de xslt trace
        for (TemplateTrace templateTrace : templateTraceList) {
            result.append(templateTrace.GetWholeTrace(true));
        }

        testTool.infopoint(correlationId, null, "Complete XSLT Trace", result.toString());
    }

    private void LoopThroughAllTemplates(String correlationId) {
        try {
            Map<Source, NodeList> templateHashMap = GetAllTemplateNodes(xslSource);
            for (Map.Entry<Source, NodeList> entry : templateHashMap.entrySet()) {
                for (int i = 0; i < entry.getValue().getLength(); i++) {
                    HandleTemplateNodes(correlationId, entry, i);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Map<Source, NodeList> GetAllTemplateNodes(StreamSource source) throws ParserConfigurationException, IOException, SAXException {
        File file = new File(source.getSystemId());
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        Document doc = documentBuilder.parse(file);
        doc.getDocumentElement().normalize();

        Map<Source, NodeList> templateHashMap = new HashMap<>();
        templateHashMap.put(source, doc.getElementsByTagName("xsl:template"));

        NodeList nodeList = doc.getElementsByTagName("xsl:import");
        if (nodeList.getLength() > 0) {
            for (int i = 0; i < nodeList.getLength(); i++) {
                Element element = (Element) nodeList.item(i);
                String importPath = element.getAttribute("href");
                templateHashMap.putAll(GetAllTemplateNodes(new StreamSource(importPath)));
            }
        }
        return templateHashMap;
    }

    private void HandleTemplateNodes(String correlationId, Map.Entry<Source, NodeList> entry, int index) {
        File file = new File(entry.getKey().getSystemId());
        Element element = (Element) entry.getValue().item(index);
        String title = entry.getValue().item(index).getNodeName() + " match=" + element.getAttribute("match");

        //Show the xsl for the template
        StringBuilder content = new StringBuilder();
        GetNodeLayout(content, element, 0, true);

        testTool.startpoint(correlationId, null, title, content.toString());

        testTool.infopoint(correlationId, null, "XSL location", "Template imported from location: " + file.getPath());

        //Show the xml that the xsl works on
        StringBuilder result = GetXMLOfTemplate(element);
        testTool.infopoint(correlationId, null, "Template XML", result.toString());

        //Show the XSLT trace
        PrintTracePerTemplate(correlationId, element.getAttribute("match"));

        testTool.endpoint(correlationId, null, title, content.toString());
    }

    private void GetNodeLayout(StringBuilder result, Node node, int indent, boolean needsIndent) {
        if (needsIndent) {
            for (int i = 0; i < indent; i++) {
                result.append("\t");
            }
        }
        if (node.getNodeType() == Node.TEXT_NODE) {
            result.append(node.getNodeValue());
        } else {
            result.append("<").append(node.getNodeName());
            if (node.hasAttributes()) {
                NamedNodeMap attributes = node.getAttributes();
                for (int j = 0; j < attributes.getLength(); j++) {
                    Node attribute = attributes.item(j);
                    result.append(" ").append(attribute.getNodeName()).append("=\"").append(attribute.getNodeValue()).append("\"");
                }
            }
            result.append(">");
            NodeList children = node.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                GetNodeLayout(result, children.item(i), indent + 1, needsIndent);
            }
            if (needsIndent) {
                for (int i = 0; i < indent; i++) {
                    result.append("\t");
                }
            }
            result.append("</").append(node.getNodeName()).append(">");
        }
    }

    private StringBuilder GetXMLOfTemplate(Node templateNode) {
        try {
            File file = new File(xmlSource.getSystemId());
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document doc = documentBuilder.parse(file);

            NodeList nodelist = doc.getElementsByTagName("*");
            Element templateElement = (Element) templateNode;
            String match = templateElement.getAttribute("match");

            StringBuilder result = new StringBuilder();
            for (int i = 0; i < nodelist.getLength(); i++) {
                if (nodelist.item(i).getNodeName().equals(match) || match.equals("/")) {
                    GetNodeLayout(result, nodelist.item(i), 0, true);
                    result.append("\n");
                }
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void PrintTracePerTemplate(String correlationId, String match){
        boolean wasUsed = true;
        for (TemplateTrace templateTrace : templateTraceList) {
            if(templateTrace.GetParentTrace().contains("match='" + match + "'") && templateTrace.GetTemplateName() != null){
                wasUsed = false;
                testTool.infopoint(correlationId, null, "match='" + match + "'", templateTrace.GetWholeTrace(false));
            }
        }
        if(wasUsed){
            testTool.infopoint(correlationId, null, "No trace match found", "No match was found to perform the XSLT on.");
        }
    }
}