package nl.nn.testtool.trace;


import lombok.Getter;
import net.sf.saxon.Controller;
import net.sf.saxon.Version;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.LetExpression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.instruct.*;
import net.sf.saxon.functions.Trace;
import net.sf.saxon.lib.*;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.s9api.Location;
import net.sf.saxon.trace.Traceable;
import net.sf.saxon.trace.TraceableComponent;
import net.sf.saxon.tree.tiny.TinyElementImpl;
import net.sf.saxon.tree.util.FastStringBuffer;
import net.sf.saxon.tree.util.Navigator;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.value.Whitespace;
import org.apache.commons.lang.NotImplementedException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class SaxonTemplateTraceListener extends StandardDiagnostics implements TraceListener, LadybugTraceListener {
    @Getter
    private final List<TemplateTrace> templateTraces;
    protected int indent = 0;
    private final int detail = 3; // none=0; low=1; normal=2; high=3
    /*@NotNull*/ private static StringBuffer spaceBuffer = new StringBuffer("                ");

    public SaxonTemplateTraceListener(){
        this.templateTraces = new ArrayList<>();
    }

    /**
     * Called at start
     *
     * @param controller controller of the transformer
     */
    @Override
    public void open(Controller controller) {
        this.templateTraces.add(new TemplateTrace("<trace " +
                "saxon-version=\"" + Version.getProductVersion() + "\" " +
                getOpeningAttributes() + ">\n"));
    }

    protected String getOpeningAttributes() {
        return "xmlns:xsl=\"" + NamespaceConstant.XSLT + '\"';
    }

    /**
     * Called at end
     */
    @Override
    public void close() {
        indent--;
        templateTraces.get(0).AddChildTrace("</trace>");
    }

    /**
     * Called when an instruction in the stylesheet gets processed
     *
     * @param info       information about the trace
     * @param properties properties of the trace
     * @param context    given xpath context
     */
    @Override
    public void enter(Traceable info, Map<String, Object> properties, XPathContext context) {
        if (isApplicable(info)) {
            trace(info, properties, context);
        }
    }

    private void trace(Traceable info, Map<String, Object> properties, XPathContext context) {
        StringBuilder trace = new StringBuilder();
        if (info instanceof Expression) {
            Expression expr = (Expression) info;
            if (expr instanceof FixedElement) {
                String tag = "LRE";
                trace.append(CreateTrace(info, tag, properties, true));
                templateTraces.get(templateTraces.size() -1).AddChildTrace(trace + "\n");
            } else if (expr instanceof FixedAttribute) {
                String tag = "ATTR";
                trace.append(CreateTrace(info, tag, properties, true));
                templateTraces.get(templateTraces.size() -1).AddChildTrace(trace + "\n");
            } else if (expr instanceof LetExpression) {
                String tag = "xsl:variable";
                trace.append(CreateTrace(info, tag, properties, true));
                templateTraces.get(templateTraces.size() -1).AddChildTrace(trace + "\n");
            } else if (expr.isCallOn(Trace.class)) {
                String tag = "fn:trace";
                trace.append(CreateTrace(info, tag, properties, true));
                templateTraces.get(templateTraces.size() -1).AddChildTrace(trace + "\n");
            } else {
                trace.append(expr.getExpressionName());
                templateTraces.get(this.templateTraces.size() - 1).AddChildTrace(trace + "\n");
            }
        } else if (info instanceof UserFunction) {
            String tag = "xsl:function";
            trace.append(CreateTrace(info, tag, properties, true));
            templateTraces.get(templateTraces.size() -1).AddChildTrace(trace + "\n");
        } else if (info instanceof TemplateRule) {
            String tag = "xsl:template match=" + ((TemplateRule) info).getMatchPattern().getOriginalText();
            trace.append(CreateTrace(info, tag, properties, false));
            templateTraces.get(templateTraces.size() - 1).AddChildTrace(trace + "\n");
            templateTraces.get(templateTraces.size() - 1).setTemplateName(((TemplateRule) info).getMatchPattern().getOriginalText());
            templateTraces.get(templateTraces.size()- 1).setSystemId(((TemplateRule) info).getSystemId());
        } else if (info instanceof NamedTemplate) {
            String tag = "xsl:template match=" + ((NamedTemplate) info).getTemplateName().getDisplayName();
            trace.append(CreateTrace(info, tag, properties, false));
            templateTraces.get(templateTraces.size() - 1).AddChildTrace(trace + "\n");
            templateTraces.get(templateTraces.size() - 1).setTemplateName(((NamedTemplate) info).getTemplateName().getDisplayName());
            templateTraces.get(templateTraces.size()- 1).setSystemId(((NamedTemplate) info).getSystemId());
        } else if (info instanceof GlobalParam) {
            String tag = "xsl:param";
            trace.append(CreateTrace(info, tag, properties, true));
            templateTraces.get(templateTraces.size() -1).AddChildTrace(trace + "\n");
        } else if (info instanceof GlobalVariable) {
            String tag = "xsl:variable";
            trace.append(CreateTrace(info, tag, properties, true));
            templateTraces.get(templateTraces.size() -1).AddChildTrace(trace + "\n");
        } else if (info instanceof Trace) {
            String tag = "fn:trace";
            trace.append(CreateTrace(info, tag, properties, true));
            templateTraces.get(templateTraces.size() -1).AddChildTrace(trace + "\n");
        } else {
            String tag = "misc";
            trace.append(CreateTrace(info, tag, properties, true));
            templateTraces.get(templateTraces.size() -1).AddChildTrace(trace + "\n");
        }
    }

    private String CreateTrace(Traceable info, String tag, Map<String, Object> properties, boolean useIndents){
        Location loc = info.getLocation();
        String file = abbreviateLocationURI(loc.getSystemId());
        StringBuilder trace = new StringBuilder();
        if(useIndents){
            trace.append(spaces(indent));
        }
        trace.append('<').append(tag).append(" ");
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            Object val = entry.getValue();
            if (val instanceof StructuredQName) {
                val = ((StructuredQName) val).getDisplayName();
            } else if (val instanceof StringValue) {
                val = ((StringValue) val).getStringValue();
            }
            if (val != null) {
                trace.append(' ').append(entry.getKey()).append("=\"").append(escape(val.toString())).append('"');
            }
        }

        trace.append(" line=\"").append(loc.getLineNumber()).append('"');

        int col = loc.getColumnNumber();
        if (col >= 0) {
            trace.append(" column=\"").append(loc.getColumnNumber()).append('"');
        }

        trace.append(" module=\"").append(escape(file)).append('"');
        trace.append(">");
        indent++;

        return trace.toString();
    }

    /**
     * Escape a string for XML output (in an attribute delimited by double quotes).
     * This method also collapses whitespace (since the value may be an XPath expression that
     * was originally written over several lines).
     *
     * @param in input string
     * @return output string
     */
    public String escape(/*@Nullable*/ String in) {
        if (in == null) {
            return "";
        }
        CharSequence collapsed = Whitespace.collapseWhitespace(in);
        FastStringBuffer sb = new FastStringBuffer(collapsed.length() + 10);
        for (int i = 0; i < collapsed.length(); i++) {
            char c = collapsed.charAt(i);
            if (c == '<') {
                sb.append("&lt;");
            } else if (c == '>') {
                sb.append("&gt;");
            } else if (c == '&') {
                sb.append("&amp;");
            } else if (c == '\"') {
                sb.append("&#34;");
            } else if (c == '\n') {
                sb.append("&#xA;");
            } else if (c == '\r') {
                sb.append("&#xD;");
            } else if (c == '\t') {
                sb.append("&#x9;");
            } else {
                sb.cat(c);
            }
        }
        return sb.toString();
    }

    /**
     * Called after an instruction of the stylesheet got processed
     *
     * @param info information about trace
     */

    @Override
    public void leave(Traceable info) {
        if (isApplicable(info)) {
            indent--;
        }
    }

    /**
     * @param info shows traceable info
     * @return bool to see if trace should be written to output stream
     */
    protected boolean isApplicable(Traceable info) {
        return level(info) <= detail;
    }

    /**
     * @param info information about the trace
     * @return the level of detail that is allowed
     */
    protected int level(Traceable info) {
        if (info instanceof TraceableComponent) {
            return 1;
        }
        if (info instanceof Instruction) {
            return 2;
        } else {
            return 3;
        }
    }

    /**
     * Called when an item becomes the context item
     *
     * @param item information about given node
     */
    @Override
    public void startCurrentItem(Item item) {
        if (item instanceof TinyElementImpl && detail > 0) {
            TinyElementImpl curr = (TinyElementImpl) item;
            this.templateTraces.add(new TemplateTrace(
                    "<source node=\"" + Navigator.getPath(curr)
                    + "\" file=\"" + curr.getSystemId()
                    + "\">\n"));
        }
        indent++;
    }

    /**
     * Called after a node of the source tree got processed
     *
     * @param item information about given node
     */
    @Override
    public void endCurrentItem(Item item) {
        indent--;
        if (item instanceof NodeInfo && detail > 0) {
            NodeInfo curr = (NodeInfo) item;
            templateTraces.get(templateTraces.size() - 1).AddChildTrace("</source><!-- " +
                    Navigator.getPath(curr) + " -->");
        }
    }

    /**
     * Get n spaces
     *
     * @param n determines how much whitespace is to be added
     * @return returns a certain amount of whitespace
     */

    protected static String spaces(int n) {
        while (spaceBuffer.length() < n) {
            spaceBuffer.append(spaceBuffer);
        }
        return spaceBuffer.substring(0, n);
    }

    /**
     * This method is deprecated
     *
     * @param stream does not do anything
     */
    @Override
    @Deprecated
    public void setOutputDestination(Logger stream) {
        throw new NotImplementedException("This method should not be used");
    }

    /**
     * This method is deprecated
     *
     * @return nothing
     */
    @Deprecated
    public Logger getOutputDestination() {
        throw new NotImplementedException("This method should not be used");
    }
}
