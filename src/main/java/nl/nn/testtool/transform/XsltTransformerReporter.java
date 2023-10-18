package nl.nn.testtool.transform;

import nl.nn.testtool.TestTool;

import javax.xml.transform.Source;

public class XsltTransformerReporter {
    private TestTool testTool;
    private Source xmlSource;
    private Source xslSource;
    public XsltTransformerReporter(TestTool testTool, Source xmlSource, Source xslSource)
    {
        this.testTool = testTool;
        this.xmlSource = xmlSource;
        this.xslSource = xslSource;
    }
    public void Start(String correlationId, String reportName)
    {
        testTool.startpoint(correlationId, null, "XSLT start point", "XSLT start point");

        //some method calls for all the input

        testTool.endpoint(correlationId, null, "XSLT end point", "XSLT end point");
    }

    private void LoopTemplates(String correlationId)
    {
        //for templateName in xslSource
            //testTool.startpoint(correlationId, null, "start of template: {templateName}", "{templateName}")
            //
    }
}
