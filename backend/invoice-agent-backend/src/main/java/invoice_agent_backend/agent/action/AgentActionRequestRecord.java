package invoice_agent_backend.agent.action;

import java.time.LocalDateTime;

/*
 ** Agent 准备执行但尚未得到用户确认的动作。
 **
 ** 这里只保存动作计划，不直接代表发票业务状态已经改变。
 */
public class AgentActionRequestRecord {

    private Long id;
    private String actionToken;
    private String dedupeKey;
    private String requestId;
    private String actionType;
    private Long taskId;
    private String decision;
    private String proposedComment;
    private String status;
    private LocalDateTime expiresAt;
    private String confirmedBy;
    private String resultSummary;
    private LocalDateTime createdAt;
    private LocalDateTime executedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getActionToken() {
        return actionToken;
    }

    public void setActionToken(String actionToken) {
        this.actionToken = actionToken;
    }

    public String getDedupeKey() {
        return dedupeKey;
    }

    public void setDedupeKey(String dedupeKey) {
        this.dedupeKey = dedupeKey;
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

    public String getConfirmedBy() {
        return confirmedBy;
    }

    public void setConfirmedBy(String confirmedBy) {
        this.confirmedBy = confirmedBy;
    }

    public String getResultSummary() {
        return resultSummary;
    }

    public void setResultSummary(String resultSummary) {
        this.resultSummary = resultSummary;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(LocalDateTime executedAt) {
        this.executedAt = executedAt;
    }
}
