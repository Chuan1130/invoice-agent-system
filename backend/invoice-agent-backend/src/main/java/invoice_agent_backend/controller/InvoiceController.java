package invoice_agent_backend.controller;

import invoice_agent_backend.common.ApiResponse;
import invoice_agent_backend.entity.AuditRuleHit;
import invoice_agent_backend.entity.AuditTask;
import invoice_agent_backend.entity.InvoiceInfo;
import invoice_agent_backend.service.AuditTaskService;
import invoice_agent_backend.vo.UploadInvoiceResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/*
 ** 前端会发一个请求：
 ** POST http://localhost:8080/api/invoices/blabla
 */
@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    // 持有一个实现了 AuditTaskService 接口的实现类。
    // 这个对象由 Spring 自动创建并注入，实际注入的是 AuditTaskServiceImpl。
    private final AuditTaskService auditTaskService;

    public InvoiceController(AuditTaskService auditTaskService) {
        this.auditTaskService = auditTaskService;
    }

    // 此文件的 key 必须是 "file" <--- @RequestParam("file")
    @PostMapping("/upload")
    public ApiResponse<UploadInvoiceResult> uploadInvoice(@RequestParam("file") MultipartFile file) {
        // 让 service 层处理具体的 upload 逻辑。
        UploadInvoiceResult result = auditTaskService.uploadInvoice(file);
        return ApiResponse.success(result);  // 用 ApiResponse.success() 包一层
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

    /*
     ** 查询某个任务命中了哪些审核规则。
     **
     ** 例如：
     ** GET /api/invoices/tasks/1/rule-hits
     */
    @GetMapping("/tasks/{id}/rule-hits")
    public ApiResponse<List<AuditRuleHit>> getRuleHitsByTaskId(@PathVariable Long id) {
        List<AuditRuleHit> ruleHits = auditTaskService.getRuleHitsByTaskId(id);
        return ApiResponse.success(ruleHits);
    }
}