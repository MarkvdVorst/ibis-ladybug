package nl.nn.testtool.transform;

import nl.nn.testtool.TestTool;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.StringWriter;

public class XsltTransformerReporter {
    private TestTool testTool;
    private StreamSource xmlSource;
    private StreamSource xslSource;

    public XsltTransformerReporter(TestTool testTool, StreamSource xmlSource, StreamSource xslSource) {
        this.testTool = testTool;
        this.xmlSource = xmlSource;
        this.xslSource = xslSource;
    }

    public void Start(String correlationId, String reportName) {
        testTool.startpoint(correlationId, null, "XSLT start point", "XSLT start point");

        try {
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer();
            transformer.transform(xmlSource, result);
            testTool.infopoint(correlationId, null, "XML Source", result.toString());
            transformer.transform(xslSource, result);
            testTool.infopoint(correlationId, null, "XML Source", result.toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        testTool.endpoint(correlationId, null, "XSLT end point", "XSLT end point");
    }

    private void LoopTemplates(String correlationId) {
        //for templateName in xslSource
        //testTool.startpoint(correlationId, null, "start of template: {templateName}", "{templateName}")
        //
    }
}
