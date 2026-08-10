package invoice_agent_backend.dto;

/*
 ** 人工审核请求
 **
 ** decision:
 ** - APPROVE
 ** - REJECT
 **
 ** reviewer:
 ** - 当前先由前端直接传审核人名称
 ** - 后面接入登录系统后，可以从登录用户中获取
 */
public class HumanReviewRequest {

    private String decision;
    private String reviewer;
    private String comment;

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public String getReviewer() {
        return reviewer;
    }

    public void setReviewer(String reviewer) {
        this.reviewer = reviewer;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}