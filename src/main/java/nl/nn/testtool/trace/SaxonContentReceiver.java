package nl.nn.testtool.trace;

import net.sf.saxon.event.ProxyReceiver;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.om.AttributeMap;
import net.sf.saxon.om.NamespaceMap;
import net.sf.saxon.om.NodeName;
import net.sf.saxon.s9api.Location;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.SchemaType;

//todo: maak dit werkent met webapp
public class SaxonContentReceiver extends ProxyReceiver {
    private final SaxonTemplateTraceListener listener;

    public SaxonContentReceiver(Receiver nextReceiver, SaxonTemplateTraceListener listener) {
        super(nextReceiver);
        this.listener = listener;
    }

    @Override
    public void startElement(NodeName elemName, SchemaType type, AttributeMap attributes, NamespaceMap namespaces, Location location, int properties) throws XPathException {
        super.startElement(elemName, type, attributes, namespaces, location, properties);
        System.out.println(elemName.getDisplayName());
    }

    @Override
    public void endElement() throws XPathException {
        super.endElement();
        System.out.println("end element");
    }
}
