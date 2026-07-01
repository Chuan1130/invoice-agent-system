package invoice_agent_backend.service;

import invoice_agent_backend.entity.AuditTask;
import invoice_agent_backend.entity.InvoiceInfo;
import invoice_agent_backend.vo.UploadInvoiceResult;
import org.springframework.web.multipart.MultipartFile;

public interface AuditTaskService {

    UploadInvoiceResult uploadInvoice(MultipartFile file);

    AuditTask getTaskById(Long id);

    InvoiceInfo getInvoiceInfoByTaskId(Long taskId);
}