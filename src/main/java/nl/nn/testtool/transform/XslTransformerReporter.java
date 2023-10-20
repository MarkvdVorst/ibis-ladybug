package nl.nn.testtool.transform;

import nl.nn.testtool.TestTool;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.lang.model.util.Elements;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;

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
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        testTool.endpoint(correlationId, null, "XSLT end point", "XSLT end point");
    }

    private void GetAllImportedXSL(String correlationId)
    {
        try {
            InputSource inputSource = new InputSource();
            inputSource.setSystemId(xslSource.getSystemId());
            File file = new File(xslSource.getSystemId());
            InputStream inputStream = new FileInputStream(file);
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document doc = documentBuilder.parse(inputStream);
            XPath xPath = XPathFactory.newInstance().newXPath();
            NodeList nodeList = (NodeList) xPath.compile("//xsl:import").evaluate(doc, XPathConstants.NODESET);

            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer();

            for(int i = 0; i < nodeList.getLength(); i++){
                Element element = (Element) nodeList.item(i);
                String importName = element.getAttribute("href");
                testTool.infopoint(correlationId, null, importName, "test");
            }

            testTool.infopoint(correlationId, null, "amount of imports", nodeList.getLength());
        }catch(Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private void LoopTemplates(String correlationId) {
        //for templateName in xslSource
        //testTool.startpoint(correlationId, null, "start of template: {templateName}", "{templateName}")
        //
    }
}
