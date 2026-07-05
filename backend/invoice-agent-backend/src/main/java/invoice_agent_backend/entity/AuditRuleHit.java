package invoice_agent_backend.entity;

import java.time.LocalDateTime;

/*
 ** AuditRuleHit 表示“一条审核规则的命中结果”。
 **
 ** 举例：
 ** 用户上传一张发票，金额是 56500。
 ** 系统规则规定：金额超过 50000 需要人工复核。
 ** 那么系统会在 audit_rule_hit 表里插入一条记录：
 **
 ** task_id = 当前任务ID
 ** rule_code = RULE_AMOUNT_LIMIT
 ** rule_name = 金额超限校验
 ** hit_result = HIT
 ** rule_message = 发票金额超过 50000 元，需要人工复核
 */
public class AuditRuleHit {

    private Long id;
    private Long taskId;
    private String ruleCode;
    private String ruleName;
    private String hitResult;
    private String ruleMessage;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getHitResult() {
        return hitResult;
    }

    public void setHitResult(String hitResult) {
        this.hitResult = hitResult;
    }

    public String getRuleMessage() {
        return ruleMessage;
    }

    public void setRuleMessage(String ruleMessage) {
        this.ruleMessage = ruleMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}