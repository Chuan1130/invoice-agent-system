package invoice_agent_backend.agent.model;

/*
 ** 用户确认 Agent 待执行动作时提交的信息。
 **
 ** confirmedBy 当前仍由请求方传入；
 ** 后续接入登录系统后应改成从认证身份中读取。
 */
public class AgentActionConfirmRequest {

    private String confirmedBy;
    private String comment;

    public String getConfirmedBy() {
        return confirmedBy;
    }

    public void setConfirmedBy(String confirmedBy) {
        this.confirmedBy = confirmedBy;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
