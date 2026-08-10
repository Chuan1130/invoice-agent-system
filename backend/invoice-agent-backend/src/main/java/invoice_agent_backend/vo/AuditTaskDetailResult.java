package invoice_agent_backend.vo;

import invoice_agent_backend.entity.AuditRuleHit;
import invoice_agent_backend.entity.AuditTask;
import invoice_agent_backend.entity.HumanReviewRecord;
import invoice_agent_backend.entity.InvoiceInfo;

import java.util.List;

/*
 ** 某个审核任务的完整详情。
 **
 ** 前端进入“任务详情页”时，
 ** 一个接口就可以把整条审核链全部拿回来。
 */
public class AuditTaskDetailResult {

    private AuditTask auditTask;

    private InvoiceInfo invoiceInfo;

    private List<AuditRuleHit> ruleHits;

    private AuditReportResult auditReport;

    private List<HumanReviewRecord> humanReviews;

    public AuditTaskDetailResult() {
    }

    public AuditTaskDetailResult(
            AuditTask auditTask,
            InvoiceInfo invoiceInfo,
            List<AuditRuleHit> ruleHits,
            AuditReportResult auditReport,
            List<HumanReviewRecord> humanReviews) {

        this.auditTask = auditTask;
        this.invoiceInfo = invoiceInfo;
        this.ruleHits = ruleHits;
        this.auditReport = auditReport;
        this.humanReviews = humanReviews;
    }

    public AuditTask getAuditTask() {
        return auditTask;
    }

    public void setAuditTask(AuditTask auditTask) {
        this.auditTask = auditTask;
    }

    public InvoiceInfo getInvoiceInfo() {
        return invoiceInfo;
    }

    public void setInvoiceInfo(InvoiceInfo invoiceInfo) {
        this.invoiceInfo = invoiceInfo;
    }

    public List<AuditRuleHit> getRuleHits() {
        return ruleHits;
    }

    public void setRuleHits(List<AuditRuleHit> ruleHits) {
        this.ruleHits = ruleHits;
    }

    public AuditReportResult getAuditReport() {
        return auditReport;
    }

    public void setAuditReport(AuditReportResult auditReport) {
        this.auditReport = auditReport;
    }

    public List<HumanReviewRecord> getHumanReviews() {
        return humanReviews;
    }

    public void setHumanReviews(
            List<HumanReviewRecord> humanReviews) {
        this.humanReviews = humanReviews;
    }
}