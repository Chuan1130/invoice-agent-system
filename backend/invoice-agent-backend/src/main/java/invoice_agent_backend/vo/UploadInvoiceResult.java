package invoice_agent_backend.vo;

import invoice_agent_backend.entity.InvoiceInfo;

/*
 ** UploadInvoiceResult 是上传接口返回给前端的 VO，也就是 View Object。
 **
 ** 之前返回：
 ** taskId / taskNo / status / originalFilePath / invoiceInfo / auditResult
 **
 ** 现在继续增加：
 ** auditReport
 **
 ** 这样前端上传成功后，可以直接看到：
 ** - 发票识别信息
 ** - 审核结论
 ** - 命中了哪些规则
 ** - 系统生成的审核报告
 */
public class UploadInvoiceResult {

    private Long taskId;
    private String taskNo;
    private String status;
    private String originalFilePath;
    private InvoiceInfo invoiceInfo;
    private AuditResult auditResult;
    private AuditReportResult auditReport;

    public UploadInvoiceResult() {
    }

    public UploadInvoiceResult(Long taskId, String taskNo, String status, String originalFilePath) {
        this.taskId = taskId;
        this.taskNo = taskNo;
        this.status = status;
        this.originalFilePath = originalFilePath;
    }

    public UploadInvoiceResult(Long taskId,
                               String taskNo,
                               String status,
                               String originalFilePath,
                               InvoiceInfo invoiceInfo) {
        this.taskId = taskId;
        this.taskNo = taskNo;
        this.status = status;
        this.originalFilePath = originalFilePath;
        this.invoiceInfo = invoiceInfo;
    }

    public UploadInvoiceResult(Long taskId,
                               String taskNo,
                               String status,
                               String originalFilePath,
                               InvoiceInfo invoiceInfo,
                               AuditResult auditResult) {
        this.taskId = taskId;
        this.taskNo = taskNo;
        this.status = status;
        this.originalFilePath = originalFilePath;
        this.invoiceInfo = invoiceInfo;
        this.auditResult = auditResult;
    }

    public UploadInvoiceResult(Long taskId,
                               String taskNo,
                               String status,
                               String originalFilePath,
                               InvoiceInfo invoiceInfo,
                               AuditResult auditResult,
                               AuditReportResult auditReport) {
        this.taskId = taskId;
        this.taskNo = taskNo;
        this.status = status;
        this.originalFilePath = originalFilePath;
        this.invoiceInfo = invoiceInfo;
        this.auditResult = auditResult;
        this.auditReport = auditReport;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getTaskNo() {
        return taskNo;
    }

    public void setTaskNo(String taskNo) {
        this.taskNo = taskNo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOriginalFilePath() {
        return originalFilePath;
    }

    public void setOriginalFilePath(String originalFilePath) {
        this.originalFilePath = originalFilePath;
    }

    public InvoiceInfo getInvoiceInfo() {
        return invoiceInfo;
    }

    public void setInvoiceInfo(InvoiceInfo invoiceInfo) {
        this.invoiceInfo = invoiceInfo;
    }

    public AuditResult getAuditResult() {
        return auditResult;
    }

    public void setAuditResult(AuditResult auditResult) {
        this.auditResult = auditResult;
    }

    public AuditReportResult getAuditReport() {
        return auditReport;
    }

    public void setAuditReport(AuditReportResult auditReport) {
        this.auditReport = auditReport;
    }
}