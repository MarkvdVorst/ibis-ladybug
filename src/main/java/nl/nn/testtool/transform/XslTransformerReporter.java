package nl.nn.testtool.transform;

import nl.nn.testtool.TestTool;
import org.apache.xalan.processor.TransformerFactoryImpl;
import org.apache.xalan.trace.PrintTraceListener;
import org.apache.xalan.trace.TraceManager;
import org.apache.xalan.transformer.TransformerImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

public class XslTransformerReporter {
    private TestTool testTool;
    private StreamSource xmlSource;
    private StreamSource xslSource;

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

            GetAllImportedXSL(correlationId);

            GetCompleteXSLT(correlationId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        testTool.endpoint(correlationId, null, "XSLT end point", "XSLT end point");
    }

    private void GetAllImportedXSL(String correlationId) {
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

    private void GetCompleteXSLT(String correlationId)
    {
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
            TraceManager traceManager = transformerImpl.getTraceManager();
            traceManager.addTraceListener(printTraceListener);

            Writer resultWriter = testTool.infopoint(correlationId, null, "XSLT Result", new StringWriter());
            transformer.transform(xmlSource, new StreamResult(resultWriter));

            printWriter.close();
            resultWriter.close();
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }
}
