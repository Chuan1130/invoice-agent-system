package invoice_agent_backend.service;

import invoice_agent_backend.vo.UploadInvoiceResult;
import org.springframework.web.multipart.MultipartFile;

public interface AuditTaskService {

    UploadInvoiceResult uploadInvoice(MultipartFile file);
}