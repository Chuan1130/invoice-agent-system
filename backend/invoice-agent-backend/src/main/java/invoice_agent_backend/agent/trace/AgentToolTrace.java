package invoice_agent_backend.agent.trace;

import java.time.LocalDateTime;

/*
 ** 一次 Tool 调用记录。
 **
 ** 当前先放在接口返回里，方便开发阶段直接观察 Supervisor
 ** 到底调用了什么。后面可以继续落库或接入统一审计日志。
 */
public class AgentToolTrace {

    private String toolName;

    private String inputSummary;

    private String resultSummary;

    private Boolean success;

    private LocalDateTime calledAt;

    public AgentToolTrace() {
    }

    public AgentToolTrace(
            String toolName,
            String inputSummary,
            String resultSummary,
            Boolean success,
            LocalDateTime calledAt) {

        this.toolName = toolName;
        this.inputSummary = inputSummary;
        this.resultSummary = resultSummary;
        this.success = success;
        this.calledAt = calledAt;
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
