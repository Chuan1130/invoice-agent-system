package invoice_agent_backend.constant;

/*
 ** 审核任务状态常量。
 **
 ** 状态含义：
 **
 ** UPLOADED
 ** 文件已经保存，任务已经创建。
 **
 ** OCR_PROCESSING
 ** 正在调用 OCR。
 **
 ** OCR_DONE
 ** OCR 成功，并且 InvoiceInfo 已经保存。
 **
 ** AUDIT_DONE
 ** 自动审核完成，但正在等待人工复核。
 **
 ** COMPLETED
 ** 整个任务已经结束。
 **
 ** FAILED
 ** OCR、规则审核或报告生成过程中发生异常。
 */
public final class AuditTaskStatus {

    public static final String UPLOADED =
            "UPLOADED";

    public static final String OCR_PROCESSING =
            "OCR_PROCESSING";

    public static final String OCR_DONE =
            "OCR_DONE";

    public static final String AUDIT_DONE =
            "AUDIT_DONE";

    public static final String COMPLETED =
            "COMPLETED";

    public static final String FAILED =
            "FAILED";

    private AuditTaskStatus() {
    }
}