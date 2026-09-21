package invoice_agent_backend.agent.model;

import invoice_agent_backend.agent.trace.AgentToolTrace;

import java.util.List;

/*
 ** Supervisor 最终返回给前端的结果。
 **
 ** answer：模型给出的审核解释或任务分析。
 ** toolTraces：本次请求实际调用过哪些业务 Tool。
 */
public class SupervisorAgentResponse {

    private String requestId;

    private String answer;

    private List<AgentToolTrace> toolTraces;

    public SupervisorAgentResponse() {
    }

    public SupervisorAgentResponse(
            String answer,
            List<AgentToolTrace> toolTraces) {

        this.answer = answer;
        this.toolTraces = toolTraces;
    }

    public String getRequestId() { return requestId; }

    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public List<AgentToolTrace> getToolTraces() {
        return toolTraces;
    }

    public void setToolTraces(
            List<AgentToolTrace> toolTraces) {
        this.toolTraces = toolTraces;
    }
}

