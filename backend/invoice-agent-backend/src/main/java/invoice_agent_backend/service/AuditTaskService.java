package invoice_agent_backend.service;

import invoice_agent_backend.common.PageResult;
import invoice_agent_backend.dto.HumanReviewRequest;
import invoice_agent_backend.entity.AuditRuleHit;
import invoice_agent_backend.entity.AuditTask;
import invoice_agent_backend.entity.InvoiceInfo;
import invoice_agent_backend.vo.AuditReportResult;
import invoice_agent_backend.vo.AuditTaskDetailResult;
import invoice_agent_backend.vo.UploadInvoiceResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AuditTaskService {

    UploadInvoiceResult uploadInvoice(MultipartFile file);

    AuditTask getTaskById(Long id);

    InvoiceInfo getInvoiceInfoByTaskId(Long taskId);

    List<AuditRuleHit> getRuleHitsByTaskId(Long taskId);

    AuditReportResult getReportByTaskId(Long taskId);

    /*
     ** 分页查看所有审核任务
     */
    PageResult<AuditTask> getTaskPage(
            String status,
            Integer page,
            Integer size
    );

    /*
     ** 一次查询整个任务详情
     */
    AuditTaskDetailResult getTaskDetail(
            Long taskId
    );

    /*
     ** 人工最终审核
     */
    AuditTaskDetailResult humanReview(
            Long taskId,
            HumanReviewRequest request
    );
}