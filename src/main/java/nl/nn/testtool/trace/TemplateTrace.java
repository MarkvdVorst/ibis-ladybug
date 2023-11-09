/*
   Copyright 2022-2023 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.testtool.trace;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

public class TemplateTrace {
    @Getter
    @Setter
    private String templateName;
    @Getter
    @Setter
    private String parentTrace;
    @Getter
    @Setter
    private String systemId;
    @Getter
    private String selectedNode;
    @Getter
    private List<String> childrenTraces;

    public TemplateTrace(String templateName, String systemId, String parentTrace) {
        this.templateName = templateName;
        this.parentTrace = parentTrace;
        this.systemId = systemId;
        this.childrenTraces = new ArrayList<>();
    }

    public TemplateTrace(String parentTrace){
        this.parentTrace = parentTrace;
        this.childrenTraces = new ArrayList<>();
    }

    /**@param nodeName selected node for the trace*/
    public void setSelectedNode(String nodeName){
        this.selectedNode = nodeName;
    }

    /**This method adds a trace to the children traces of the parent template trace
     * @param childTrace adds a child trace of this parent trace*/
    public void AddChildTrace(String childTrace) {
        childrenTraces.add(childTrace);
    }

    /**@param index of child trace
     * @return Get a child trace from a specific index*/
    public String getChildTrace(int index) {
        return childrenTraces.get(index);
    }

    /**Empties the child traces for this template trace*/
    public void Flush() {
        childrenTraces = new ArrayList<>();
    }

    /**@param showSeperator determines whether it shows a line to separate the traces.
     * @return Returns a string that holds the complete trace of the transform*/
    public String getWholeTrace(boolean showSeperator){
        StringBuilder result = new StringBuilder();

        if(showSeperator) {
            result.append("--------------------------------------------New template being applied--------------------------------------------\n");
        }
        result.append(parentTrace);

        for (String childrenTrace : childrenTraces) {
            result.append(childrenTrace);
        }

        return result.toString();
    }
}
