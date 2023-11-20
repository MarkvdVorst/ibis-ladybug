package nl.nn.testtool.trace;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import java.util.Arrays;

public class SaxonContentHandler implements ContentHandler {
    private final SaxonTemplateTraceListener traceListener;

    public SaxonContentHandler(SaxonTemplateTraceListener traceListener){
        this.traceListener = traceListener;
    }

    @Override
    public void setDocumentLocator(Locator locator) {
        System.out.println(locator.toString());
    }

    @Override
    public void startDocument() throws SAXException {
        System.out.println("startDocument");
    }

    @Override
    public void endDocument() throws SAXException {
        System.out.println("endDocument");
    }

    @Override
    public void startPrefixMapping(String s, String s1) throws SAXException {
        System.out.println("\nstart prefix mapping: " + s + " " + s1);
    }

    @Override
    public void endPrefixMapping(String s) throws SAXException {
        System.out.println("\nend prefix mapping: " + s);
    }

    @Override
    public void startElement(String s, String s1, String s2, Attributes attributes) throws SAXException {
        traceListener.startElement(s1);
        System.out.println("\nstart element: " + s + " " + s1 + " " + s2);
    }

    @Override
    public void endElement(String s, String s1, String s2) throws SAXException {
        traceListener.endElement(s2);
        System.out.println("\nend element: " + s + " " + s1 + " " + s2);
    }

    @Override
    public void characters(char[] chars, int i, int i1) throws SAXException {
        StringBuilder result = new StringBuilder();

        for (char aChar : chars) {
            result.append(aChar);
        }
        System.out.println("\ncharacters: " + result);
    }

    @Override
    public void ignorableWhitespace(char[] chars, int i, int i1) throws SAXException {
        StringBuilder result = new StringBuilder();

        for (char aChar : chars) {
            result.append(aChar);
        }
        System.out.println("\nignorable whitespace: " + result);
    }

    @Override
    public void processingInstruction(String s, String s1) throws SAXException {
        System.out.println("\nprocessing instructions: " + s + " " + "s1");
    }

    @Override
    public void skippedEntity(String s) throws SAXException {
        System.out.println("\nskipped entity: " + s);
    }
}
