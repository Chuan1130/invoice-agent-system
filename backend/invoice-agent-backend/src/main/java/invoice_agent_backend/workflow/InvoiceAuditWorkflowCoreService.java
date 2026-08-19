package invoice_agent_backend.workflow;

import invoice_agent_backend.vo.UploadInvoiceResult;
import invoice_agent_backend.workflow.state.AuditTaskState;

/*
 ** 发票审核核心业务事务。
 **
 ** 负责：
 **
 ** 1. OCR
 ** 2. 保存 InvoiceInfo
 ** 3. 执行审核规则
 ** 4. 保存审核结论
 ** 5. 生成审核报告
 **
 ** 这些操作位于同一个主事务中。
 */
public interface InvoiceAuditWorkflowCoreService {

    UploadInvoiceResult execute(
            AuditTaskState state
    );
}