package invoice_agent_backend.service;

import invoice_agent_backend.entity.InvoiceInfo;

public interface OcrService {

    InvoiceInfo recognizeInvoice(Long taskId, String filePath);
}