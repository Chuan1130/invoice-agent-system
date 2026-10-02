package invoice_agent_backend.agent.model;

import invoice_agent_backend.agent.trace.AgentRequestRecord;
import invoice_agent_backend.agent.trace.AgentToolTrace;

import java.util.List;

/*
 ** 按 requestId 查询一次 Agent 执行时返回的聚合结果。
 **
 ** request 保存整次 Supervisor 请求的生命周期，
 ** toolTraces 保存这次请求实际调用过的业务 Tool。
 */
public class AgentRequestTraceResult {

    private AgentRequestRecord request;
    private List<AgentToolTrace> toolTraces;

    public AgentRequestTraceResult() {
    }

    public AgentRequestTraceResult(
            AgentRequestRecord request,
            List<AgentToolTrace> toolTraces) {

        this.request = request;
        this.toolTraces = toolTraces;
    }

    public AgentRequestRecord getRequest() {
        return request;
    }

    public void setRequest(AgentRequestRecord request) {
        this.request = request;
    }

    public List<AgentToolTrace> getToolTraces() {
        return toolTraces;
    }

    public void setToolTraces(List<AgentToolTrace> toolTraces) {
        this.toolTraces = toolTraces;
    }
}
