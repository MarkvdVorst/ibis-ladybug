package nl.nn.testtool.trace;


import net.sf.saxon.Controller;
import net.sf.saxon.Version;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.trace.AbstractTraceListener;
import net.sf.saxon.trace.Traceable;

import java.util.Map;

public class SaxonTemplateTraceListener extends AbstractTraceListener {
    private int detail = 2;
    private Controller controller;

    @Override
    public void open(Controller controller) {
        out.info("<trace " +
                "saxon-version=\"" + Version.getProductVersion() + "\" " +
                getOpeningAttributes() + '>');
        indent++;
        this.controller = controller;
    }

    @Override
    protected String getOpeningAttributes() {
        return null;
    }

    @Override
    protected String tag(Traceable info) {
        return info.toString();
    }

    @Override
    public void startCurrentItem(Item item){
        if(item instanceof NodeInfo && detail > 0){
            NodeInfo curr = (NodeInfo) item;
            System.out.println(curr.getDisplayName());
            System.out.println(curr.getSystemId());
        }
    }
}
