package invoice_agent_backend.workflow;

import invoice_agent_backend.vo.UploadInvoiceResult;
import org.springframework.web.multipart.MultipartFile;

/*
 ** 发票完整审核工作流
 **
 ** Controller 不需要知道 OCR、规则、报告如何执行。
 ** AuditTaskService 也不需要再承担整个流程。
 */
public interface InvoiceAuditWorkflowService {

    UploadInvoiceResult execute(MultipartFile file);
}