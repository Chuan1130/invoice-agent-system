package invoice_agent_backend.controller;

import invoice_agent_backend.common.ApiResponse;
import invoice_agent_backend.common.PageResult;
import invoice_agent_backend.dto.HumanReviewRequest;
import invoice_agent_backend.entity.AuditRuleHit;
import invoice_agent_backend.entity.AuditTask;
import invoice_agent_backend.entity.InvoiceInfo;
import invoice_agent_backend.service.AuditTaskService;
import invoice_agent_backend.vo.AuditReportResult;
import invoice_agent_backend.vo.AuditTaskDetailResult;
import invoice_agent_backend.vo.UploadInvoiceResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final AuditTaskService auditTaskService;

    public InvoiceController(
            AuditTaskService auditTaskService) {

        this.auditTaskService =
                auditTaskService;
    }

    /*
     ** 上传发票并启动完整审核 Workflow。
     */
    @PostMapping("/upload")
    public ApiResponse<UploadInvoiceResult>
    uploadInvoice(
            @RequestParam("file")
            MultipartFile file) {

        UploadInvoiceResult result =
                auditTaskService
                        .uploadInvoice(file);

        return ApiResponse.success(result);
    }

    /*
     ** 任务列表。
     **
     ** 示例：
     **
     ** GET /api/invoices/tasks
     **
     ** GET /api/invoices/tasks?page=1&size=20
     **
     ** GET /api/invoices/tasks
     **     ?status=AUDIT_DONE&page=1&size=20
     */
    @GetMapping("/tasks")
    public ApiResponse<PageResult<AuditTask>>
    getTaskPage(

            @RequestParam(
                    required = false)
            String status,

            @RequestParam(
                    defaultValue = "1")
            Integer page,

            @RequestParam(
                    defaultValue = "20")
            Integer size) {

        return ApiResponse.success(
                auditTaskService
                        .getTaskPage(
                                status,
                                page,
                                size
                        )
        );
    }

    @GetMapping("/tasks/{id}")
    public ApiResponse<AuditTask>
    getTaskById(
            @PathVariable Long id) {

        return ApiResponse.success(
                auditTaskService
                        .getTaskById(id)
        );
    }

    @GetMapping(
            "/tasks/{id}/invoice-info")
    public ApiResponse<InvoiceInfo>
    getInvoiceInfoByTaskId(
            @PathVariable Long id) {

        return ApiResponse.success(
                auditTaskService
                        .getInvoiceInfoByTaskId(id)
        );
    }

    @GetMapping(
            "/tasks/{id}/rule-hits")
    public ApiResponse<List<AuditRuleHit>>
    getRuleHitsByTaskId(
            @PathVariable Long id) {

        return ApiResponse.success(
                auditTaskService
                        .getRuleHitsByTaskId(id)
        );
    }

    @GetMapping(
            "/tasks/{id}/report")
    public ApiResponse<AuditReportResult>
    getReportByTaskId(
            @PathVariable Long id) {

        return ApiResponse.success(
                auditTaskService
                        .getReportByTaskId(id)
        );
    }

    /*
     ** 一个接口查看完整任务详情。
     */
    @GetMapping(
            "/tasks/{id}/detail")
    public ApiResponse<AuditTaskDetailResult>
    getTaskDetail(
            @PathVariable Long id) {

        return ApiResponse.success(
                auditTaskService
                        .getTaskDetail(id)
        );
    }

    /*
     ** 人工复核接口。
     **
     ** POST
     ** /api/invoices/tasks/1/human-review
     */
    @PostMapping(
            "/tasks/{id}/human-review")
    public ApiResponse<AuditTaskDetailResult>
    humanReview(

            @PathVariable Long id,

            @RequestBody
            HumanReviewRequest request) {

        return ApiResponse.success(
                auditTaskService
                        .humanReview(
                                id,
                                request
                        )
        );
    }
}