package invoice_agent_backend.service.impl;

import invoice_agent_backend.common.PageResult;
import invoice_agent_backend.constant.AuditDecision;
import invoice_agent_backend.constant.AuditTaskStatus;
import invoice_agent_backend.dto.HumanReviewRequest;
import invoice_agent_backend.entity.AuditRuleHit;
import invoice_agent_backend.entity.AuditTask;
import invoice_agent_backend.entity.HumanReviewRecord;
import invoice_agent_backend.entity.InvoiceInfo;
import invoice_agent_backend.mapper.AuditRuleHitMapper;
import invoice_agent_backend.mapper.AuditTaskMapper;
import invoice_agent_backend.mapper.HumanReviewRecordMapper;
import invoice_agent_backend.mapper.InvoiceInfoMapper;
import invoice_agent_backend.service.AuditReportService;
import invoice_agent_backend.service.AuditTaskService;
import invoice_agent_backend.vo.AuditReportResult;
import invoice_agent_backend.vo.AuditResult;
import invoice_agent_backend.vo.AuditTaskDetailResult;
import invoice_agent_backend.vo.UploadInvoiceResult;
import invoice_agent_backend.workflow.InvoiceAuditWorkflowService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/*
 ** AuditTaskService 现在主要负责：
 **
 ** 1. 调用 Workflow 执行完整审核
 ** 2. 查询任务
 ** 3. 查询任务详情
 ** 4. 人工复核
 **
 ** 真正的上传 → OCR → 规则 → 报告，
 ** 已经交给 InvoiceAuditWorkflowService。
 */
@Service
public class AuditTaskServiceImpl
        implements AuditTaskService {

    private final InvoiceAuditWorkflowService workflowService;

    private final AuditTaskMapper auditTaskMapper;

    private final InvoiceInfoMapper invoiceInfoMapper;

    private final AuditRuleHitMapper auditRuleHitMapper;

    private final HumanReviewRecordMapper humanReviewRecordMapper;

    private final AuditReportService auditReportService;

    public AuditTaskServiceImpl(
            InvoiceAuditWorkflowService workflowService,
            AuditTaskMapper auditTaskMapper,
            InvoiceInfoMapper invoiceInfoMapper,
            AuditRuleHitMapper auditRuleHitMapper,
            HumanReviewRecordMapper humanReviewRecordMapper,
            AuditReportService auditReportService) {

        this.workflowService = workflowService;
        this.auditTaskMapper = auditTaskMapper;
        this.invoiceInfoMapper = invoiceInfoMapper;
        this.auditRuleHitMapper = auditRuleHitMapper;
        this.humanReviewRecordMapper = humanReviewRecordMapper;
        this.auditReportService = auditReportService;
    }

    /*
     ** 上传接口现在只负责启动 Workflow。
     */
    @Override
    public UploadInvoiceResult uploadInvoice(
            MultipartFile file) {

        return workflowService.execute(file);
    }

    @Override
    public AuditTask getTaskById(Long id) {

        if (id == null) {
            throw new RuntimeException(
                    "任务ID不能为空"
            );
        }

        AuditTask auditTask =
                auditTaskMapper
                        .selectAuditTaskById(id);

        if (auditTask == null) {
            throw new RuntimeException(
                    "任务不存在"
            );
        }

        return auditTask;
    }

    @Override
    public InvoiceInfo getInvoiceInfoByTaskId(
            Long taskId) {

        if (taskId == null) {
            throw new RuntimeException(
                    "任务ID不能为空"
            );
        }

        InvoiceInfo invoiceInfo =
                invoiceInfoMapper
                        .selectInvoiceInfoByTaskId(taskId);

        if (invoiceInfo == null) {
            throw new RuntimeException(
                    "发票信息不存在"
            );
        }

        return invoiceInfo;
    }

    @Override
    public List<AuditRuleHit>
    getRuleHitsByTaskId(Long taskId) {

        if (taskId == null) {
            throw new RuntimeException(
                    "任务ID不能为空"
            );
        }

        return auditRuleHitMapper
                .selectRuleHitsByTaskId(taskId);
    }

    /*
     ** 查询之前已经生成好的报告。
     **
     ** 这里删除了原来那个：
     **
     ** InvoiceInfo invoiceInfo =
     **     getInvoiceInfoByTaskId(taskId);
     **
     ** 因为查出来以后根本没有使用。
     */
    @Override
    public AuditReportResult getReportByTaskId(
            Long taskId) {

        AuditTask auditTask =
                getTaskById(taskId);

        if (auditTask.getReportPath() == null
                || auditTask.getReportPath()
                .trim()
                .isEmpty()) {

            throw new RuntimeException(
                    "该任务还没有生成审核报告"
            );
        }

        String reportContent =
                auditReportService
                        .readReportContent(
                                auditTask.getReportPath()
                        );

        return new AuditReportResult(
                auditTask.getId(),
                auditTask.getTaskNo(),
                auditTask.getFinalDecision(),
                auditTask.getNeedHumanReview(),
                auditTask.getReportPath(),
                reportContent
        );
    }

    /*
     ** 任务列表分页查询。
     **
     ** page 默认 1
     ** size 默认 20
     ** size 最大 100
     */
    @Override
    public PageResult<AuditTask> getTaskPage(
            String status,
            Integer page,
            Integer size) {

        int safePage =
                page == null || page < 1
                        ? 1
                        : page;

        int safeSize =
                size == null || size < 1
                        ? 20
                        : Math.min(size, 100);

        int offset =
                (safePage - 1) * safeSize;

        String safeStatus =
                status == null
                        ? null
                        : status.trim();

        List<AuditTask> records =
                auditTaskMapper
                        .selectAuditTaskPage(
                                safeStatus,
                                offset,
                                safeSize
                        );

        long total =
                auditTaskMapper
                        .countAuditTasks(
                                safeStatus
                        );

        return new PageResult<>(
                safePage,
                safeSize,
                total,
                records
        );
    }

    /*
     ** 聚合任务详情。
     **
     ** 前端不再需要分别请求：
     ** task
     ** invoice-info
     ** rule-hits
     ** report
     ** human-review
     */
    @Override
    public AuditTaskDetailResult getTaskDetail(
            Long taskId) {

        AuditTask auditTask =
                getTaskById(taskId);

        InvoiceInfo invoiceInfo =
                invoiceInfoMapper
                        .selectInvoiceInfoByTaskId(taskId);

        List<AuditRuleHit> ruleHits =
                auditRuleHitMapper
                        .selectRuleHitsByTaskId(taskId);

        List<HumanReviewRecord> humanReviews =
                humanReviewRecordMapper
                        .selectByTaskId(taskId);

        AuditReportResult auditReport = null;

        if (auditTask.getReportPath() != null
                && !auditTask.getReportPath()
                .trim()
                .isEmpty()) {

            String reportContent =
                    auditReportService
                            .readReportContent(
                                    auditTask.getReportPath()
                            );

            auditReport =
                    new AuditReportResult(
                            auditTask.getId(),
                            auditTask.getTaskNo(),
                            auditTask.getFinalDecision(),
                            auditTask.getNeedHumanReview(),
                            auditTask.getReportPath(),
                            reportContent
                    );
        }

        return new AuditTaskDetailResult(
                auditTask,
                invoiceInfo,
                ruleHits,
                auditReport,
                humanReviews
        );
    }

    /*
     ** Human-in-the-loop
     **
     ** 系统发现风险：
     **
     ** NEED_HUMAN_REVIEW
     **
     **        ↓
     **
     ** 人工审核
     **
     ** APPROVE / REJECT
     **
     **        ↓
     **
     ** APPROVED_BY_HUMAN
     ** 或
     ** REJECTED_BY_HUMAN
     */
    @Override
    @Transactional
    public AuditTaskDetailResult humanReview(
            Long taskId,
            HumanReviewRequest request) {

        if (request == null) {
            throw new RuntimeException(
                    "人工审核参数不能为空"
            );
        }

        if (request.getDecision() == null
                || request.getDecision()
                .trim()
                .isEmpty()) {

            throw new RuntimeException(
                    "人工审核 decision 不能为空"
            );
        }

        AuditTask auditTask =
                getTaskById(taskId);

        /*
         ** 只有确实需要人工复核的任务，
         ** 才能进入人工审核。
         */
        if (!Boolean.TRUE.equals(
                auditTask.getNeedHumanReview())) {

            throw new RuntimeException(
                    "该任务当前不需要人工复核"
            );
        }

        String decision =
                request.getDecision()
                        .trim()
                        .toUpperCase();

        String finalDecision;

        if ("APPROVE".equals(decision)) {

            finalDecision =
                    AuditDecision
                            .APPROVED_BY_HUMAN;

        } else if ("REJECT".equals(decision)) {

            finalDecision =
                    AuditDecision
                            .REJECTED_BY_HUMAN;

        } else {

            throw new RuntimeException(
                    "decision 只支持 APPROVE 或 REJECT"
            );
        }

        /*
         ** 先保存人工审核记录。
         */
        HumanReviewRecord record =
                new HumanReviewRecord();

        record.setTaskId(taskId);

        record.setDecision(decision);

        record.setReviewer(
                request.getReviewer()
        );

        record.setReviewComment(
                request.getComment()
        );

        humanReviewRecordMapper
                .insertHumanReviewRecord(record);

        /*
         ** 更新任务的最终审核状态。
         */
        auditTaskMapper
                .updateHumanReviewResult(
                        taskId,
                        AuditTaskStatus.COMPLETED,
                        finalDecision,
                        false
                );

        auditTask.setStatus(
                AuditTaskStatus.COMPLETED
        );

        auditTask.setFinalDecision(
                finalDecision
        );

        auditTask.setNeedHumanReview(false);

        /*
         ** 人工审核以后，
         ** 原来的报告已经不是最终报告。
         **
         ** 因此重新生成报告。
         */
        InvoiceInfo invoiceInfo =
                getInvoiceInfoByTaskId(taskId);

        List<AuditRuleHit> ruleHits =
                getRuleHitsByTaskId(taskId);

        AuditResult finalAuditResult =
                new AuditResult(
                        finalDecision,
                        false,
                        ruleHits
                );

        AuditReportResult newReport =
                auditReportService
                        .generateReport(
                                auditTask,
                                invoiceInfo,
                                finalAuditResult
                        );

        auditTaskMapper.updateReportPath(
                taskId,
                newReport.getReportPath()
        );

        auditTask.setReportPath(
                newReport.getReportPath()
        );

        return getTaskDetail(taskId);
    }
}