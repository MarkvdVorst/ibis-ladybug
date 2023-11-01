package nl.nn.testtool.trace;

import java.util.ArrayList;
import java.util.List;

public class TemplateTrace {
    private final String _templateName;
    private final String _parentTrace;
    private final String _systemId;
    private String _selectedNode;
    private List<String> _childrenTraces;

    public TemplateTrace(String templateName, String systemId, String parentTrace) {
        this._templateName = templateName;
        this._parentTrace = parentTrace;
        this._systemId = systemId;
        this._childrenTraces = new ArrayList<>();
    }

    /**@param nodeName selected node for the trace*/
    public void setSelectedNode(String nodeName){
        this._selectedNode = nodeName;
    }

    /**@return returns the selected node of this trace*/
    public String getSelectedNode(){
        return _selectedNode;
    }

    /**@return Gets the systemId of the template*/
    public String getTemplateName(){
        return _templateName;
    }

    public String getSystemId() {
        return this._systemId;
    }

    /**@return Gets the entire first trace that mentions the template location and the match for it*/
    public String getParentTrace() {
        return _parentTrace;
    }

    /**This method adds a trace to the children traces of the parent template trace
     * @param childTrace adds a child trace of this parent trace*/
    public void AddChildTrace(String childTrace) {
        _childrenTraces.add(childTrace);
    }

    /**@param index of child trace
     * @return Get a child trace from a specific index*/
    public String getChildTrace(int index) {
        return _childrenTraces.get(index);
    }

    /**@return returns all children traces*/
    public List<String> getAllChildTraces() {
        return _childrenTraces;
    }

    /**Empties the child traces for this template trace*/
    public void Flush() {
        _childrenTraces = new ArrayList<>();
    }

    /**@param showSeperator determines whether it shows a line to separate the traces.
     * @return Returns a string that holds the complete trace of the transform*/
    public String getWholeTrace(boolean showSeperator){
        StringBuilder result = new StringBuilder();

        if(showSeperator) {
            result.append("--------------------------------------------New template being applied--------------------------------------------");
        }
        result.append(_parentTrace);

        for (String childrenTrace : _childrenTraces) {
            result.append(childrenTrace);
        }

        return result.toString();
    }
}
