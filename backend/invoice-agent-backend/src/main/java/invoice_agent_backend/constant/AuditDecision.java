package invoice_agent_backend.constant;

/*
 ** 审核最终结论常量
 */
public final class AuditDecision {

    // 规则没有发现风险，系统自动通过
    public static final String APPROVED = "APPROVED";

    // 规则发现风险，需要人工继续审核
    public static final String NEED_HUMAN_REVIEW = "NEED_HUMAN_REVIEW";

    // 人工审核后确认通过
    public static final String APPROVED_BY_HUMAN = "APPROVED_BY_HUMAN";

    // 人工审核后确认拒绝
    public static final String REJECTED_BY_HUMAN = "REJECTED_BY_HUMAN";

    private AuditDecision() {
    }
}