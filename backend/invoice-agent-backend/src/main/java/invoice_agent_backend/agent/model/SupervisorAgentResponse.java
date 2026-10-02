package invoice_agent_backend.agent.model;

import invoice_agent_backend.agent.trace.AgentToolTrace;

import java.util.List;

/*
 ** Supervisor 最终返回给前端的结果。
 **
 ** answer 保留兼容旧前端；
 ** structuredAnswer 是新的结构化模型输出；
 ** pendingActions 由 Java Runtime 直接附加，不依赖模型复制 confirmationToken。
 */
public class SupervisorAgentResponse {

    private String requestId;
    private String answer;
    private SupervisorStructuredAnswer structuredAnswer;
    private List<AgentPendingActionView> pendingActions;
    private List<AgentToolTrace> toolTraces;

    public SupervisorAgentResponse() {
    }

    public SupervisorAgentResponse(
            String requestId,
            String answer,
            List<AgentToolTrace> toolTraces) {

        this(
                requestId,
                answer,
                null,
                List.of(),
                toolTraces
        );
    }

    public SupervisorAgentResponse(
            String requestId,
            String answer,
            SupervisorStructuredAnswer structuredAnswer,
            List<AgentPendingActionView> pendingActions,
            List<AgentToolTrace> toolTraces) {

        this.requestId = requestId;
        this.answer = answer;
        this.structuredAnswer = structuredAnswer;
        this.pendingActions = pendingActions;
        this.toolTraces = toolTraces;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public SupervisorStructuredAnswer getStructuredAnswer() {
        return structuredAnswer;
    }

    public void setStructuredAnswer(
            SupervisorStructuredAnswer structuredAnswer) {
        this.structuredAnswer = structuredAnswer;
    }

    public List<AgentPendingActionView> getPendingActions() {
        return pendingActions;
    }

    public void setPendingActions(
            List<AgentPendingActionView> pendingActions) {
        this.pendingActions = pendingActions;
    }

    public List<AgentToolTrace> getToolTraces() {
        return toolTraces;
    }

    public void setToolTraces(
            List<AgentToolTrace> toolTraces) {
        this.toolTraces = toolTraces;
    }
}
