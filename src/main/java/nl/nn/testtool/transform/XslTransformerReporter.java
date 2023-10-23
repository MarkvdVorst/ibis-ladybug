package nl.nn.testtool.transform;

import nl.nn.testtool.TestTool;
import org.apache.xalan.processor.TransformerFactoryImpl;
import org.apache.xalan.trace.PrintTraceListener;
import org.apache.xalan.trace.TraceManager;
import org.apache.xalan.transformer.TransformerImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
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
import java.util.Map;

public class XslTransformerReporter {
    private TestTool testTool;
    private StreamSource xmlSource;
    private StreamSource xslSource;
    private TraceManager traceManager;

    public XslTransformerReporter(TestTool testTool, StreamSource xmlSource, StreamSource xslSource) {
        this.testTool = testTool;
        this.xmlSource = xmlSource;
        this.xslSource = xslSource;
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
        Writer writer = testTool.infopoint(correlationId, null, "Complete XSLT", new StringWriter());
        PrintWriter printWriter = new PrintWriter(writer);
        PrintTraceListener printTraceListener = new PrintTraceListener(printWriter);

        printTraceListener.m_traceElements = true;
        printTraceListener.m_traceGeneration = true;
        printTraceListener.m_traceSelection = true;
        printTraceListener.m_traceTemplates = true;
        printTraceListener.m_traceExtension = true;

        try {
            TransformerFactoryImpl transformerFactory = new TransformerFactoryImpl();
            Transformer transformer = transformerFactory.newTransformer(xslSource);
            TransformerImpl transformerImpl = (TransformerImpl) transformer;
            this.traceManager = transformerImpl.getTraceManager();
            this.traceManager.addTraceListener(printTraceListener);

            Writer resultWriter = testTool.infopoint(correlationId, null, "XSLT Result", new StringWriter());
            transformer.transform(xmlSource, new StreamResult(resultWriter));

            printWriter.close();
            resultWriter.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void LoopThroughAllTemplates(String correlationId) {
        //TODO zet een nodelist van alle templates in de tracemanager van Get5CompleteXSLT. Overleg met Yaseen en Jaco of dit een goed idee is en of er andere opties zijn.
        StringBuilder stringBuilder = new StringBuilder();

        try {
            Map<Source, NodeList> templateHashMap = GetAllTemplateNodes(xslSource);
//            for (NodeList templateNode : templateHashMap) {
//                for (int j = 0; j < templateNode.getLength(); j++) {
//                    NewTemplateNode(correlationId, (Element) templateNode.item(j));
//                }
//            }
            for (Map.Entry<Source, NodeList> entry : templateHashMap.entrySet()) {
                for (int i = 0; i < entry.getValue().getLength(); i++) {
                    File file = new File(entry.getKey().getSystemId());
                    Element element = (Element) entry.getValue().item(i);
                    String title = entry.getValue().item(i).getNodeName() + " match=" + element.getAttribute("match");
                    String content = "Template imported from location: " + file.getPath();
                    testTool.startpoint(correlationId, null, title, content);

                    testTool.endpoint(correlationId, null, title, content);
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
}
