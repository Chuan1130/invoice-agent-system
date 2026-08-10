package invoice_agent_backend.vo;

/*
 ** AuditReportResult 是返回给前端的审核报告结果对象。
 **
 ** 作用：
 ** - 告诉前端这份报告属于哪个任务
 ** - 告诉前端最终审核结论是什么
 ** - 告诉前端报告文件保存在哪里
 ** - 同时也可以直接把报告内容返回给前端展示
 */
public class AuditReportResult {

    private Long taskId;
    private String taskNo;
    private String finalDecision;
    private Boolean needHumanReview;
    private String reportPath;
    private String reportContent;

    public AuditReportResult() {
    }

    public AuditReportResult(Long taskId,
                             String taskNo,
                             String finalDecision,
                             Boolean needHumanReview,
                             String reportPath,
                             String reportContent) {
        this.taskId = taskId;
        this.taskNo = taskNo;
        this.finalDecision = finalDecision;
        this.needHumanReview = needHumanReview;
        this.reportPath = reportPath;
        this.reportContent = reportContent;
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

    public String getReportPath() {
        return reportPath;
    }

    public void setReportPath(String reportPath) {
        this.reportPath = reportPath;
    }

    public String getReportContent() {
        return reportContent;
    }

    public void setReportContent(String reportContent) {
        this.reportContent = reportContent;
    }
}