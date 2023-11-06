package nl.nn.testtool.transform;

import nl.nn.testtool.TestTool;
import nl.nn.testtool.trace.TemplateTrace;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class XslTransformerReporterSaxon {
    private final TestTool testTool;
    private final File xmlFile;
    private final File xslFile;
    private final String xsltResult;
    private final List<TemplateTrace> templateTraceList;
    private List<File> allXSLFiles;

    public XslTransformerReporterSaxon(TestTool testTool, File xmlFile, File xslFile, List<TemplateTrace> templateTraceStack, String xsltResult) {
        this.testTool = testTool;
        this.xmlFile = xmlFile;
        this.xslFile = xslFile;
        this.templateTraceList = templateTraceStack;
        this.xsltResult = xsltResult;
        this.allXSLFiles = new ArrayList<>();
        this.allXSLFiles.add(xslFile);
    }

    public void start(){

    }
}
