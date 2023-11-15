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
import java.util.Map;

public class TemplateTrace {
    @Getter
    @Setter
    private String traceId;
    @Getter
    private TemplateTrace parentTrace;
    @Getter
    @Setter
    private String templateMatch;
    @Getter
    private String templateTrace;
    @Getter
    @Setter
    private String systemId;
    @Getter
    private List<String> traceContext;
    @Getter
    private List<TemplateTrace> childTraces;
    @Getter
    @Setter
    private String selectedNode;
    @Getter
    @Setter
    private boolean aBuiltInTemplate;

    public TemplateTrace(String templateMatch, String systemId, String templateTrace, String id, TemplateTrace parentTrace) {
        this.templateMatch = templateMatch;
        this.templateTrace = templateTrace;
        this.systemId = systemId;
        this.traceContext = new ArrayList<>();
        this.childTraces = new ArrayList<>();
        this.traceId = id;
        this.parentTrace = parentTrace;
    }

    public TemplateTrace(String templateTrace, TemplateTrace parentTrace){
        this.templateTrace = templateTrace;
        this.traceContext = new ArrayList<>();
        this.childTraces = new ArrayList<>();
        this.parentTrace = parentTrace;
    }

    public TemplateTrace(){
        this.traceContext = new ArrayList<>();
        this.childTraces = new ArrayList<>();
    }

    public void addChildtrace(TemplateTrace trace){
        this.childTraces.add(trace);
    }

    /**This method adds a trace to the children traces of the parent template trace
     * @param context adds a context trace to this trace*/
    public void addTraceContext(String context) {
        this.traceContext.add(context);
    }

    /**@param showSeparator determines whether it shows a line to separate the traces.
     * @return Returns a string that holds the complete trace of the transform*/
    public String getWholeTrace(boolean showSeparator){
        StringBuilder result = new StringBuilder();

        if(showSeparator) {
            result.append("--------------------------------------------New template being applied--------------------------------------------\n");
        }
        result.append(templateTrace);

        for (String childrenTrace : traceContext) {
            result.append(childrenTrace);
        }

        return result.toString();
    }
}
