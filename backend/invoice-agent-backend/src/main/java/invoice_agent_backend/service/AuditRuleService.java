package invoice_agent_backend.service;

import invoice_agent_backend.entity.InvoiceInfo;
import invoice_agent_backend.vo.AuditResult;

/*
 ** 审核规则服务接口。
 **
 ** 输入：
 ** - OCR 识别出来的 InvoiceInfo
 **
 ** 输出：
 ** - AuditResult 审核结果
 */
public interface AuditRuleService {

    AuditResult checkRules(InvoiceInfo invoiceInfo);
}