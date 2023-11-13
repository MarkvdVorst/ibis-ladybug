package nl.nn.testtool.util;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

public class DocumentUtil {

    private static DocumentBuilderFactory newDocumentBuilderFactory(){
        return DocumentBuilderFactory.newInstance();
    }

    public static DocumentBuilder newDocumentBuilder() throws ParserConfigurationException {
        return newDocumentBuilderFactory().newDocumentBuilder();
    }
    public static Document buildDocument(File file) throws ParserConfigurationException, IOException, SAXException {
        Document newDocument = newDocumentBuilder().parse(file);
        newDocument.getDocumentElement().normalize();
        return newDocument;
    }
}
