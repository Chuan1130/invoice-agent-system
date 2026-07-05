package invoice_agent_backend.vo;

import invoice_agent_backend.entity.AuditRuleHit;

import java.util.List;

/*
 ** AuditResult 是返回给前端的审核结果对象。
 **
 ** finalDecision:
 ** - APPROVED：系统自动通过
 ** - NEED_HUMAN_REVIEW：需要人工复核
 **
 ** needHumanReview:
 ** - true：需要人工看
 ** - false：系统可以自动通过
 **
 ** ruleHits:
 ** - 保存具体命中了哪些规则
 */
public class AuditResult {

    private String finalDecision;
    private Boolean needHumanReview;
    private List<AuditRuleHit> ruleHits;

    public AuditResult() {
    }

    public AuditResult(String finalDecision, Boolean needHumanReview, List<AuditRuleHit> ruleHits) {
        this.finalDecision = finalDecision;
        this.needHumanReview = needHumanReview;
        this.ruleHits = ruleHits;
    }

    public String getFinalDecision() {
        return finalDecision;
    }

    public void setFinalDecision(String finalDecision) {
        this.finalDecision = finalDecision;
    }

    public Boolean getNeedHumanReview() {
        return needHumanReview;
    }

    public void setNeedHumanReview(Boolean needHumanReview) {
        this.needHumanReview = needHumanReview;
    }

    public List<AuditRuleHit> getRuleHits() {
        return ruleHits;
    }

    public void setRuleHits(List<AuditRuleHit> ruleHits) {
        this.ruleHits = ruleHits;
    }
}