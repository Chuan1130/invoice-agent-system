package invoice_agent_backend.service;

import invoice_agent_backend.entity.AuditRuleHit;
import invoice_agent_backend.entity.AuditTask;
import invoice_agent_backend.entity.InvoiceInfo;
import invoice_agent_backend.vo.UploadInvoiceResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AuditTaskService {

    UploadInvoiceResult uploadInvoice(MultipartFile file);

    AuditTask getTaskById(Long id);

    InvoiceInfo getInvoiceInfoByTaskId(Long taskId);

    List<AuditRuleHit> getRuleHitsByTaskId(Long taskId);
}