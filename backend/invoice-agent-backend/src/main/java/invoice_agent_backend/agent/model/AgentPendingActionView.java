package invoice_agent_backend.agent.model;

import invoice_agent_backend.agent.action.AgentActionRequestRecord;

import java.time.LocalDateTime;

/*
 ** 返回给前端的待确认动作视图。
 **
 ** confirmationToken 只用于用户确认当前动作，
 ** 不交给模型作为业务执行凭证。
 */
public class AgentPendingActionView {

    private String confirmationToken;
    private String requestId;
    private String actionType;
    private Long taskId;
    private String decision;
    private String proposedComment;
    private String status;
    private LocalDateTime expiresAt;
    private LocalDateTime executedAt;

    public AgentPendingActionView() {
    }

    public static AgentPendingActionView from(
            AgentActionRequestRecord record) {

        if (record == null) {
            return null;
        }

        AgentPendingActionView view =
                new AgentPendingActionView();

        view.setConfirmationToken(
                record.getActionToken()
        );
        view.setRequestId(record.getRequestId());
        view.setActionType(record.getActionType());
        view.setTaskId(record.getTaskId());
        view.setDecision(record.getDecision());
        view.setProposedComment(
                record.getProposedComment()
        );
        view.setStatus(record.getStatus());
        view.setExpiresAt(record.getExpiresAt());
        view.setExecutedAt(record.getExecutedAt());

        return view;
    }

    public String getConfirmationToken() {
        return confirmationToken;
    }

    public void setConfirmationToken(String confirmationToken) {
        this.confirmationToken = confirmationToken;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public String getProposedComment() {
        return proposedComment;
    }

    public void setProposedComment(String proposedComment) {
        this.proposedComment = proposedComment;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(LocalDateTime executedAt) {
        this.executedAt = executedAt;
    }
}
