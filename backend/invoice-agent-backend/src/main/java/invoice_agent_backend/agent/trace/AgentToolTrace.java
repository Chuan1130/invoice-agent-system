package invoice_agent_backend.agent.trace;

import java.time.LocalDateTime;

/*
 ** 一次 Tool 调用记录。
 **
 ** requestId 用来把同一次 Supervisor 请求里的多次 Tool 调用串起来。
 ** 这样后面即使一个请求调用多个 Tool，也能完整还原执行链。
 */
public class AgentToolTrace {

    private Long id;

    private String requestId;

    private String toolName;

    private String inputSummary;

    private String resultSummary;

    private Boolean success;

    private LocalDateTime calledAt;

    public AgentToolTrace() {
    }

    public AgentToolTrace(
            String requestId,
            String toolName,
            String inputSummary,
            String resultSummary,
            Boolean success,
            LocalDateTime calledAt) {

        this.requestId = requestId;
        this.toolName = toolName;
        this.inputSummary = inputSummary;
        this.resultSummary = resultSummary;
        this.success = success;
        this.calledAt = calledAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public String getInputSummary() {
        return inputSummary;
    }

    public void setInputSummary(String inputSummary) {
        this.inputSummary = inputSummary;
    }

    public String getResultSummary() {
        return resultSummary;
    }

    public void setResultSummary(String resultSummary) {
        this.resultSummary = resultSummary;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public LocalDateTime getCalledAt() {
        return calledAt;
    }

    public void setCalledAt(LocalDateTime calledAt) {
        this.calledAt = calledAt;
    }
}
