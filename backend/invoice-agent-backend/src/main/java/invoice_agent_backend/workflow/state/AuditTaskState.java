package invoice_agent_backend.workflow.state;

import invoice_agent_backend.entity.AuditTask;
import invoice_agent_backend.entity.InvoiceInfo;
import invoice_agent_backend.vo.AuditReportResult;
import invoice_agent_backend.vo.AuditResult;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/*
 ** 整个发票审核 Workflow 的状态对象。
 **
 ** 可以把它理解成：
 **
 ** 一张发票从上传开始，在不同处理节点之间传递的“档案袋”。
 **
 ** 后面升级 Spring AI Alibaba Graph 时，
 ** 这个类就可以继续演化成真正的 Graph State。
 */
public class AuditTaskState {

    private File targetFile;

    private AuditTask auditTask;

    private InvoiceInfo invoiceInfo;

    private AuditResult auditResult;

    private AuditReportResult auditReport;

    private final List<String> completedSteps = new ArrayList<>();

    public void completeStep(String step) {
        completedSteps.add(step);
    }

    public File getTargetFile() {
        return targetFile;
    }

    public void setTargetFile(File targetFile) {
        this.targetFile = targetFile;
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

    public List<String> getCompletedSteps() {
        return completedSteps;
    }
}