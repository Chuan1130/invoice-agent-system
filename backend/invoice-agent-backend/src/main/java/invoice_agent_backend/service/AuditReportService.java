package invoice_agent_backend.service;

import invoice_agent_backend.entity.AuditTask;
import invoice_agent_backend.entity.InvoiceInfo;
import invoice_agent_backend.vo.AuditReportResult;
import invoice_agent_backend.vo.AuditResult;

/*
 ** 审核报告服务接口。
 **
 ** 输入：
 ** - auditTask：当前审核任务
 ** - invoiceInfo：OCR 识别出来的发票信息
 ** - auditResult：规则审核后的结果
 **
 ** 输出：
 ** - AuditReportResult 审核报告结果
 */
public interface AuditReportService {

    AuditReportResult generateReport(AuditTask auditTask,
                                     InvoiceInfo invoiceInfo,
                                     AuditResult auditResult);

    String readReportContent(String reportPath);
}