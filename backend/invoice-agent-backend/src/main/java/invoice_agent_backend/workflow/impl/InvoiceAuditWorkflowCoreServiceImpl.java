package invoice_agent_backend.workflow.impl;

import invoice_agent_backend.constant.AuditTaskStatus;
import invoice_agent_backend.entity.AuditTask;
import invoice_agent_backend.entity.InvoiceInfo;
import invoice_agent_backend.mapper.AuditTaskMapper;
import invoice_agent_backend.mapper.InvoiceInfoMapper;
import invoice_agent_backend.service.AuditReportService;
import invoice_agent_backend.service.AuditRuleService;
import invoice_agent_backend.service.AuditTaskLifecycleService;
import invoice_agent_backend.service.OcrService;
import invoice_agent_backend.vo.AuditReportResult;
import invoice_agent_backend.vo.AuditResult;
import invoice_agent_backend.vo.UploadInvoiceResult;
import invoice_agent_backend.workflow.InvoiceAuditWorkflowCoreService;
import invoice_agent_backend.workflow.state.AuditTaskState;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 ** 发票审核核心 Workflow。
 **
 ** 这个类中的 execute 使用主事务。
 **
 ** 如果 OCR、规则审核、数据库保存或报告生成失败，
 ** 这里的数据库操作全部回滚。
 **
 ** 外层 Orchestrator 捕获异常以后，
 ** 再通过 REQUIRES_NEW 把任务更新为 FAILED。
 */
@Service
public class InvoiceAuditWorkflowCoreServiceImpl
        implements InvoiceAuditWorkflowCoreService {

    private final AuditTaskMapper auditTaskMapper;

    private final InvoiceInfoMapper invoiceInfoMapper;

    private final OcrService ocrService;

    private final AuditRuleService auditRuleService;

    private final AuditReportService auditReportService;

    private final AuditTaskLifecycleService
            auditTaskLifecycleService;

    public InvoiceAuditWorkflowCoreServiceImpl(
            AuditTaskMapper auditTaskMapper,
            InvoiceInfoMapper invoiceInfoMapper,
            OcrService ocrService,
            AuditRuleService auditRuleService,
            AuditReportService auditReportService,
            AuditTaskLifecycleService auditTaskLifecycleService) {

        this.auditTaskMapper =
                auditTaskMapper;

        this.invoiceInfoMapper =
                invoiceInfoMapper;

        this.ocrService =
                ocrService;

        this.auditRuleService =
                auditRuleService;

        this.auditReportService =
                auditReportService;

        this.auditTaskLifecycleService =
                auditTaskLifecycleService;
    }

    /*
     ** Core 主事务。
     **
     ** 注意：
     ** 创建任务和 OCR_PROCESSING 不在这个事务中。
     **
     ** 它们已经由外层通过 REQUIRES_NEW 提前提交。
     */
    @Override
    @Transactional
    public UploadInvoiceResult execute(
            AuditTaskState state) {

        validateState(state);

        /*
         ** Node 1：
         ** OCR 识别并保存 InvoiceInfo。
         */
        executeOcr(state);

        /*
         ** Node 2：
         ** 执行审核规则并决定任务状态。
         */
        executeAuditRules(state);

        /*
         ** Node 3：
         ** 生成审核报告。
         */
        generateAuditReport(state);

        /*
         ** Node 4：
         ** 构造上传接口返回结果。
         */
        return buildResult(state);
    }

    /*
     ** 检查外层传进来的 Workflow State。
     */
    private void validateState(
            AuditTaskState state) {

        if (state == null) {
            throw new RuntimeException(
                    "Workflow State 不能为空"
            );
        }

        if (state.getAuditTask() == null
                || state.getAuditTask()
                .getId() == null) {

            throw new RuntimeException(
                    "Workflow 中的审核任务不能为空"
            );
        }

        if (state.getTargetFile() == null) {
            throw new RuntimeException(
                    "Workflow 中的发票文件不能为空"
            );
        }
    }

    /*
     ** OCR_PROCESSING
     **        ↓
     ** OCR_DONE
     */
    private void executeOcr(
            AuditTaskState state) {

        AuditTask auditTask =
                state.getAuditTask();

        InvoiceInfo invoiceInfo =
                ocrService.recognizeInvoice(
                        auditTask.getId(),
                        state.getTargetFile()
                                .getAbsolutePath()
                );

        if (invoiceInfo == null) {
            throw new RuntimeException(
                    "OCR 未返回发票信息"
            );
        }

        int invoiceRows =
                invoiceInfoMapper
                        .insertInvoiceInfo(
                                invoiceInfo
                        );

        if (invoiceRows != 1) {
            throw new RuntimeException(
                    "保存 OCR 发票信息失败"
            );
        }

        int taskRows =
                auditTaskMapper
                        .updateTaskAfterOcr(
                                auditTask.getId(),
                                invoiceInfo.getRawJson()
                        );

        if (taskRows != 1) {
            throw new RuntimeException(
                    "保存 OCR 原始内容失败"
            );
        }

        /*
         ** 状态转换统一经过状态机。
         **
         ** transitionStatus 使用 REQUIRED，
         ** 所以这里会加入当前 Core 主事务。
         */
        auditTaskLifecycleService
                .transitionStatus(
                        auditTask.getId(),
                        AuditTaskStatus.OCR_DONE
                );

        auditTask.setStatus(
                AuditTaskStatus.OCR_DONE
        );

        auditTask.setOcrRawText(
                invoiceInfo.getRawJson()
        );

        state.setInvoiceInfo(
                invoiceInfo
        );

        state.completeStep(
                "OCR_DONE"
        );
    }

    /*
     ** 执行规则审核。
     **
     ** 有风险：
     ** OCR_DONE -> AUDIT_DONE
     **
     ** 无风险：
     ** OCR_DONE -> COMPLETED
     */
    private void executeAuditRules(
            AuditTaskState state) {

        AuditTask auditTask =
                state.getAuditTask();

        AuditResult auditResult =
                auditRuleService.checkRules(
                        state.getInvoiceInfo()
                );

        if (auditResult == null) {
            throw new RuntimeException(
                    "审核规则没有返回结果"
            );
        }

        boolean needHumanReview =
                Boolean.TRUE.equals(
                        auditResult
                                .getNeedHumanReview()
                );

        /*
         ** 决定自动审核后的真实任务状态。
         */
        String targetStatus =
                needHumanReview
                        ? AuditTaskStatus.AUDIT_DONE
                        : AuditTaskStatus.COMPLETED;

        int affectedRows =
                auditTaskMapper
                        .updateAuditResult(
                                auditTask.getId(),
                                auditResult
                                        .getFinalDecision(),
                                needHumanReview
                        );

        if (affectedRows != 1) {
            throw new RuntimeException(
                    "保存自动审核结果失败"
            );
        }

        /*
         ** 使用状态机完成：
         **
         ** OCR_DONE -> AUDIT_DONE
         ** 或
         ** OCR_DONE -> COMPLETED
         */
        auditTaskLifecycleService
                .transitionStatus(
                        auditTask.getId(),
                        targetStatus
                );

        auditTask.setStatus(
                targetStatus
        );

        auditTask.setFinalDecision(
                auditResult
                        .getFinalDecision()
        );

        auditTask.setNeedHumanReview(
                needHumanReview
        );

        state.setAuditResult(
                auditResult
        );

        state.completeStep(
                "RULE_AUDIT_DONE"
        );

        if (AuditTaskStatus.COMPLETED
                .equals(targetStatus)) {

            state.completeStep(
                    "AUTO_COMPLETED"
            );
        }
    }

    /*
     ** 生成审核报告。
     **
     ** 报告会使用 AuditTask 当前真实状态：
     **
     ** AUDIT_DONE
     ** 或
     ** COMPLETED
     */
    private void generateAuditReport(
            AuditTaskState state) {

        AuditReportResult auditReport =
                auditReportService
                        .generateReport(
                                state.getAuditTask(),
                                state.getInvoiceInfo(),
                                state.getAuditResult()
                        );

        if (auditReport == null
                || auditReport
                .getReportPath() == null
                || auditReport
                .getReportPath()
                .trim()
                .isEmpty()) {

            throw new RuntimeException(
                    "审核报告生成失败"
            );
        }

        int affectedRows =
                auditTaskMapper
                        .updateReportPath(
                                state.getAuditTask()
                                        .getId(),
                                auditReport
                                        .getReportPath()
                        );

        if (affectedRows != 1) {
            throw new RuntimeException(
                    "保存审核报告路径失败"
            );
        }

        state.getAuditTask()
                .setReportPath(
                        auditReport
                                .getReportPath()
                );

        state.setAuditReport(
                auditReport
        );

        state.completeStep(
                "REPORT_GENERATED"
        );
    }

    private UploadInvoiceResult buildResult(
            AuditTaskState state) {

        AuditTask auditTask =
                state.getAuditTask();

        return new UploadInvoiceResult(
                auditTask.getId(),
                auditTask.getTaskNo(),
                auditTask.getStatus(),
                auditTask.getOriginalFilePath(),
                state.getInvoiceInfo(),
                state.getAuditResult(),
                state.getAuditReport()
        );
    }
}