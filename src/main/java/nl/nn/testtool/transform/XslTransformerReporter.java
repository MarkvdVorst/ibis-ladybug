/*
   Copyright 2023 WeAreFrank!

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
import nl.nn.testtool.util.DocumentUtil;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

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
    private final TemplateTrace rootTrace;
    private List<File> allXSLFiles;
    private final String correlationId;
    private final String reportName;

    public XslTransformerReporter(TestTool testTool, File xmlFile, File xslFile, TemplateTrace rootTrace, String xsltResult, String correlationId, String reportName) {
        this.testTool = testTool;
        this.xmlFile = xmlFile;
        this.xslFile = xslFile;
        this.rootTrace = rootTrace;
        this.xsltResult = xsltResult;
        this.allXSLFiles = new ArrayList<>();
        this.allXSLFiles.add(this.xslFile);
        this.correlationId = correlationId;
        this.reportName = reportName;
    }

    public static void initiate(TestTool testTool, File xmlFile, File xslFile, TemplateTrace rootTrace, String xsltResult, String correlationId, String reportName){
        XslTransformerReporter reporter = new XslTransformerReporter(testTool, xmlFile, xslFile, rootTrace, xsltResult, correlationId, reportName);
        testTool.startpoint(correlationId, null, reportName, "XSLT Trace");
        reporter.start();
        testTool.endpoint(correlationId, null, reportName, "XSLT Trace");
    }

    private void start() {
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

            printImportedXsl();

            printCompleteTraceFromStack(rootTrace);

            printTransformedXml();

            testTool.startpoint(correlationId, null, "Trace layout", null);
            loopThroughAllTemplates(rootTrace);
            testTool.endpoint(correlationId, null, "Trace layout", null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        testTool.endpoint(correlationId, xmlFile.getName(), "Start XSLT", "End of XSLT");
    }


    private void printImportedXsl() {
        try {

            Document xslDocument = DocumentUtil.buildDocument(xslFile);
            if(!fileHasNode("import", xslDocument)) {
                return;
            }

            NodeList nodeList = getNodesByXPath("//*[local-name()='import']",xslDocument);
            testTool.startpoint(correlationId, xslFile.getName(), "Imported XSL", "Imported XSL files");
            // Loop over all the 'import' nodes (each node references 1 XSL file in its 'href' attribute)
            for (int i = 0; i < nodeList.getLength(); i++) {
                Element element = (Element) nodeList.item(i); // Get the import element from current import node
                String importPath = element.getAttribute("href"); // Grab the file path from the 'href' attribute
                Path xslFilePath = Paths.get(importPath);
                this.allXSLFiles.add(xslFilePath.toFile()); // Add the imported XSL file to global variable for later reference
                writeFileToInfopoint(xslFilePath); //write the entire XSL file to the report as an infopoint
            }
            testTool.endpoint(correlationId, xslFile.getName(), "Imported XSL", "Imported XSL files");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void writeFileToInfopoint(Path filepath) throws IOException {
        StringWriter writer = new StringWriter();
        for (String xsl : DocumentUtil.readFile(filepath)) {
            writer.append(xsl).append("\n");
        }
        testTool.infopoint(correlationId, xslFile.getName(), filepath.getFileName().toString(), writer.toString());
    }


    /**
    * @param nodeName should be the name of the node to look for WITHOUT namespace prefix
    * */
    private boolean fileHasNode(String nodeName, Document doc) {
        try {
            // Check if a node with the provided name exists is populated
            if (getNodesByXPath("//*[local-name()='"+ nodeName +"']", doc).getLength() == 0) {
                return false;
            }
            return true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void printTransformedXml() {
        testTool.infopoint(correlationId, xmlFile.getName(), "XML after full transformation", xsltResult);
    }

    //TODO: nog niet af
    private void printCompleteTraceFromStack(TemplateTrace trace) {
        String result = getAllTraces(trace);
        testTool.infopoint(correlationId, xslFile.getName(), "Complete XSLT Trace", result);
    }

    private String getAllTraces(TemplateTrace trace){
        StringBuilder result = new StringBuilder();
        if(trace.getChildTraces().isEmpty()) return "";
        for (TemplateTrace templateTrace : trace.getChildTraces()) {
            result.append(templateTrace.getWholeTrace(true)).append("\n");
            result.append(getAllTraces(templateTrace));
        }
        return result.toString();
    }

    /*
    * This method iterates over all instances of 'template match' nodes
    * */
    //TODO: nog niet af
    private void loopThroughAllTemplates(TemplateTrace trace) {
        try {
            if(trace.getChildTraces().isEmpty()) return;
            for (TemplateTrace templateTrace : trace.getChildTraces()) {
                if(!templateTrace.isABuiltInTemplate()) {
                    testTool.startpoint(correlationId, templateTrace.getTraceId(), "template match=" + templateTrace.getTemplateMatch(), templateTrace.getWholeTrace(false));
                    PrintXSLOfTemplate(templateTrace.getTemplateMatch());
                    loopThroughAllTemplates(templateTrace);
                    testTool.endpoint(correlationId, templateTrace.getTraceId(), "template match=" + templateTrace.getTemplateMatch(), templateTrace.getWholeTrace(false));
                } else {
                    testTool.startpoint(correlationId, templateTrace.getTraceId(), "built-in-rule match=" + templateTrace.getTemplateMatch() + " node=" + templateTrace.getSelectedNode(), templateTrace.getWholeTrace(false));
                    PrintXSLOfTemplate(templateTrace.getTemplateMatch());
                    loopThroughAllTemplates(templateTrace);
                    testTool.endpoint(correlationId, templateTrace.getTraceId(), "built-in-rule match=" + templateTrace.getTemplateMatch() + " node=" + templateTrace.getSelectedNode(), templateTrace.getWholeTrace(false));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void printTemplateXsl(String templateName) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {

        for (File file : allXSLFiles) {
            boolean hasMatchAttribute = false;
            Document doc = DocumentUtil.buildDocument(file);
            NodeList nodeList = getNodesByXPath("//*[local-name()='template']", doc);
            StringWriter result = new StringWriter();

            for (int i = 0; i < nodeList.getLength(); i++) {
                Element element = (Element) nodeList.item(i);

                if (element.getAttribute("match").equals(templateName)) {
                    hasMatchAttribute = true;
                    StringBuilder stringBuilder = new StringBuilder();
                    getNodeIndentation(stringBuilder, nodeList.item(i), 0, true);
                    result.append(stringBuilder).append("\n");
                }
            }
            if (!hasMatchAttribute) continue;

            testTool.infopoint(correlationId, null, file.getName(), result.toString());
        }
    }

    private StringBuilder getTemplateXml(Node templateNode) {
        try {
            Document doc = DocumentUtil.buildDocument(xmlFile);

            NodeList nodelist = doc.getElementsByTagName("*");
            Element templateElement = (Element) templateNode;
            String match = templateElement.getAttribute("match");

            StringBuilder result = new StringBuilder();
            for (int i = 0; i < nodelist.getLength(); i++) {
                if (nodelist.item(i).getNodeName().equals(match) || match.equals("/")) {
                    getNodeIndentation(result, nodelist.item(i), 0, true);
                    result.append("\n");
                }
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void getNodeIndentation(StringBuilder result, Node node, int indent, boolean needsIndent) {
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
                getNodeIndentation(result, children.item(i), indent + 1, needsIndent);
            }
            if (needsIndent) {
                for (int i = 0; i < indent; i++) {
                    result.append("\t");
                }
            }
            result.append("</").append(node.getNodeName()).append(">");
        }
    }

    private NodeList getNodesByXPath(String xPathExpression, Document doc) throws XPathExpressionException {
        XPath xpath = XPathFactory.newInstance().newXPath();
        XPathExpression expression = xpath.compile(xPathExpression);
        return (NodeList) expression.evaluate(doc.getDocumentElement(), XPathConstants.NODESET);
    }

}