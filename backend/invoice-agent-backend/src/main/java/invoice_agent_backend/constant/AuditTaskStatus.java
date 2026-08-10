package invoice_agent_backend.constant;

/*
 ** 审核任务状态常量
 **
 ** 用常量代替业务代码里到处出现的字符串，
 ** 避免 "AUDIT_DONE"、"OCR_DONE" 拼错。
 */
public final class AuditTaskStatus {

    public static final String UPLOADED = "UPLOADED";

    public static final String OCR_DONE = "OCR_DONE";

    public static final String AUDIT_DONE = "AUDIT_DONE";

    /*
     ** COMPLETED 表示整个任务已经真正结束。
     **
     ** 例如：
     ** - 系统自动通过
     ** - 或者人工已经完成最终审核
     */
    public static final String COMPLETED = "COMPLETED";

    private AuditTaskStatus() {
    }
}