package invoice_agent_backend.agent.model;

import invoice_agent_backend.vo.AuditTaskDetailResult;

/*
 ** 用户确认一个 Agent 动作后的结果。
 **
 ** executedNow=false 可能表示动作已经执行过、已取消或已过期。
 ** 同一个 confirmationToken 重复确认不会重复执行业务写操作。
 */
public class AgentActionExecutionResult {

    private AgentPendingActionView action;
    private AuditTaskDetailResult taskDetail;
    private Boolean executedNow;
    private String message;

    public AgentActionExecutionResult() {
    }

    public AgentActionExecutionResult(
            AgentPendingActionView action,
            AuditTaskDetailResult taskDetail,
            Boolean executedNow,
            String message) {

        this.action = action;
        this.taskDetail = taskDetail;
        this.executedNow = executedNow;
        this.message = message;
    }

    public AgentPendingActionView getAction() {
        return action;
    }

    public void setAction(AgentPendingActionView action) {
        this.action = action;
    }

    public AuditTaskDetailResult getTaskDetail() {
        return taskDetail;
    }

    public void setTaskDetail(AuditTaskDetailResult taskDetail) {
        this.taskDetail = taskDetail;
    }

    public Boolean getExecutedNow() {
        return executedNow;
    }

    public void setExecutedNow(Boolean executedNow) {
        this.executedNow = executedNow;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
