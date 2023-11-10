/*
   Copyright 2022-2023 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.testtool.transform;

import nl.nn.testtool.trace.TemplateTrace;
import nl.nn.testtool.TestTool;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class XslTransformerReporter {
    private final TestTool testTool;
    private final File xmlFile;
    private final File xslFile;
    private final String xsltResult;
    private final List<TemplateTrace> templateTraceList;
    private List<File> allXSLFiles;

    public XslTransformerReporter(TestTool testTool, File xmlFile, File xslFile, List<TemplateTrace> templateTraceStack, String xsltResult) {
        //TODO: gebruik files
        this.testTool = testTool;
        this.xmlFile = xmlFile;
        this.xslFile = xslFile;
        this.templateTraceList = templateTraceStack;
        this.xsltResult = xsltResult;
        this.allXSLFiles = new ArrayList<>();
        this.allXSLFiles.add(xslFile);
    }

    public void Start(String correlationId, String reportName) {
        testTool.startpoint(correlationId, xmlFile.getName(), "Start XSLT", "Start XSLT");
        try {
            List<String> xmlList = Files.readAllLines(Paths.get(xmlFile.getAbsolutePath()));
            StringWriter writer = new StringWriter();
            for (String xml : xmlList) {
                writer.append(xml).append("\n");
            }
            testTool.infopoint(correlationId, null, "XML input file", writer.toString());

            List<String> xslList = Files.readAllLines(Paths.get(xslFile.getAbsolutePath()));
            writer = new StringWriter();
            for (String xsl : xslList) {
                writer.append(xsl).append("\n");
            }
            testTool.infopoint(correlationId, xmlFile.getName(), "XSL input file", writer.toString());

            PrintAllImportedXSL(correlationId);

            PrintCompleteTraceFromStack(correlationId);

            PrintCompleteXSLT(correlationId);

            LoopThroughAllTemplates(correlationId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        testTool.endpoint(correlationId, xmlFile.getName(), "Start XSLT", "End of XSLT");
    }

    //TODO refactor into multiple methods according to Single Responsibility (at least separate 'reader' and 'writer')
    private void PrintAllImportedXSL(String correlationId) {
        try {
            Document xslDocument = BuildDocument(xslFile);
            if(!ImportNodesPresent(xslDocument)) {
                return;
            }
            NodeList nodeList = GetNodesByXPath("//*[local-name()='import']",xslDocument);
            testTool.startpoint(correlationId, xslFile.getName(), "Imported XSL", "Imported XSL files");
            // Loop over all the 'import' nodes (each node references 1 XSL file)
            for (int i = 0; i < nodeList.getLength(); i++) {
                // Get the import element from current import node
                Element element = (Element) nodeList.item(i);
                // Grab the file path from the 'href' attribute
                String importPath = element.getAttribute("href");
                Path xslFilePath = Paths.get(importPath);
                // Get the XSL file's name using the given path
                String fileName = xslFilePath.getFileName().toString();

                // Add the imported XSL file to global variable
                this.allXSLFiles.add(xslFilePath.toFile());

                // Read all the lines in the imported XSL file to a List and write them into the report one by one
                List<String> xslList = Files.readAllLines(xslFilePath);
                StringWriter writer = new StringWriter();
                for (String xsl : xslList) {
                    writer.append(xsl).append("\n");
                }
                testTool.infopoint(correlationId, xslFile.getName(), fileName, writer.toString());
            }
            testTool.endpoint(correlationId, xslFile.getName(), "Imported XSL", "Imported XSL files");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean ImportNodesPresent(Document doc) {
        try {
            // Get a list of all 'import' nodes
            NodeList nodeList = GetNodesByXPath("//*[local-name()='import']", doc);
            // Check if nodeList is populated
            if (nodeList.getLength() == 0) {
                return false;
            }
            return true;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void PrintCompleteXSLT(String correlationId) {
        testTool.infopoint(correlationId, xmlFile.getName(), "XML after full transformation", xsltResult);
    }

    private void PrintCompleteTraceFromStack(String correlationId) {
        StringBuilder result = new StringBuilder();
        for (TemplateTrace templateTrace : templateTraceList) {
            result.append(templateTrace.getWholeTrace(true)).append("\n");
        }
        testTool.infopoint(correlationId, xslFile.getName(), "Complete XSLT Trace", result.toString());
    }

    /*
    * This method iterates over all instances of 'template match' nodes
    * */
    private void LoopThroughAllTemplates(String correlationId) {
        try {

            for (TemplateTrace templateTrace : templateTraceList) {
//                System.out.println(templateTrace.getSelectedNode());
                if(templateTrace.getSelectedNode() == null) {
                    testTool.startpoint(correlationId, null, "template match=" + templateTrace.getTemplateName(), templateTrace.getWholeTrace(false));
                    PrintXSLOfTemplate(correlationId, templateTrace.getTemplateName());
                    testTool.endpoint(correlationId, null, "template match=" + templateTrace.getTemplateName(), templateTrace.getWholeTrace(false));
                } else {
                    testTool.startpoint(correlationId, null, "built-in-rule match=" + templateTrace.getTemplateName() + " node=" + templateTrace.getSelectedNode(), templateTrace.getWholeTrace(false));
                    PrintXSLOfTemplate(correlationId, templateTrace.getTemplateName());
                    testTool.endpoint(correlationId, null, "built-in-rule match=" + templateTrace.getTemplateName() + " node=" + templateTrace.getSelectedNode(), templateTrace.getWholeTrace(false));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void PrintXSLOfTemplate(String correlationId, String templateName) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc;
        for (File file : allXSLFiles) {
            boolean wasFound = false;
            doc = builder.parse(file);
            doc.getDocumentElement().normalize();
            NodeList nodeList = GetNodesByXPath("//*[local-name()='template']", doc);
            StringWriter result = new StringWriter();
            for (int i = 0; i < nodeList.getLength(); i++) {
                Element element = (Element) nodeList.item(i);

                if (element.getAttribute("match").equals(templateName)) {
                    wasFound = true;
                    StringBuilder stringBuilder = new StringBuilder();
                    GetNodeLayout(stringBuilder, nodeList.item(i), 0, true);
                    result.append(stringBuilder).append("\n");
                }
            }
            if (!wasFound) continue;

            testTool.infopoint(correlationId, null, file.getName(), result.toString());
        }
    }

    private StringBuilder GetXMLOfTemplate(Node templateNode) {
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document doc = documentBuilder.parse(xmlFile);

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

    private NodeList GetNodesByXPath(String xPathExpression, Document doc) throws XPathExpressionException {
        XPath xpath = XPathFactory.newInstance().newXPath();
        XPathExpression expression = xpath.compile(xPathExpression);
        return (NodeList) expression.evaluate(doc.getDocumentElement(), XPathConstants.NODESET);
    }

    private Document BuildDocument(File file) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        Document doc = documentBuilder.parse(file);
        doc.getDocumentElement().normalize();
        return doc;
    }

}