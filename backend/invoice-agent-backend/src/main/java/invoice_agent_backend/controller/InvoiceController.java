package invoice_agent_backend.controller;

import invoice_agent_backend.common.ApiResponse;
import invoice_agent_backend.entity.AuditTask;
import invoice_agent_backend.entity.InvoiceInfo;
import invoice_agent_backend.service.AuditTaskService;
import invoice_agent_backend.vo.UploadInvoiceResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final AuditTaskService auditTaskService;

    public InvoiceController(AuditTaskService auditTaskService) {
        this.auditTaskService = auditTaskService;
    }

    // 此文件的key必须是"file" <--- @RequestParam("file")
    @PostMapping("/upload")
    public ApiResponse<UploadInvoiceResult> uploadInvoice(@RequestParam("file") MultipartFile file) {
        UploadInvoiceResult result = auditTaskService.uploadInvoice(file);
        return ApiResponse.success(result);
    }

    @GetMapping("/tasks/{id}")
    public ApiResponse<AuditTask> getTaskById(@PathVariable Long id) {
        AuditTask auditTask = auditTaskService.getTaskById(id);
        return ApiResponse.success(auditTask);
    }

    @GetMapping("/tasks/{id}/invoice-info")
    public ApiResponse<InvoiceInfo> getInvoiceInfoByTaskId(@PathVariable Long id) {
        InvoiceInfo invoiceInfo = auditTaskService.getInvoiceInfoByTaskId(id);
        return ApiResponse.success(invoiceInfo);
    }
}