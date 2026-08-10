package invoice_agent_backend.workflow.impl;

import invoice_agent_backend.constant.AuditTaskStatus;
import invoice_agent_backend.entity.AuditTask;
import invoice_agent_backend.entity.InvoiceInfo;
import invoice_agent_backend.mapper.AuditTaskMapper;
import invoice_agent_backend.mapper.InvoiceInfoMapper;
import invoice_agent_backend.service.AuditReportService;
import invoice_agent_backend.service.AuditRuleService;
import invoice_agent_backend.service.OcrService;
import invoice_agent_backend.vo.AuditReportResult;
import invoice_agent_backend.vo.AuditResult;
import invoice_agent_backend.vo.UploadInvoiceResult;
import invoice_agent_backend.workflow.InvoiceAuditWorkflowService;
import invoice_agent_backend.workflow.state.AuditTaskState;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/*
 ** 发票审核 Workflow。
 **
 ** 以前这些逻辑全部堆在 AuditTaskServiceImpl.uploadInvoice()。
 **
 ** 现在把整个流程拆成多个明确节点：
 **
 ** validateUpload
 **      ↓
 ** saveInvoiceFile
 **      ↓
 ** createAuditTask
 **      ↓
 ** executeOcr
 **      ↓
 ** executeAuditRules
 **      ↓
 ** generateAuditReport
 **      ↓
 ** buildResult
 **
 ** 后面接入 Agent Graph 时，
 ** 这些方法可以进一步转换成 Graph Node。
 */
@Service
public class InvoiceAuditWorkflowServiceImpl
        implements InvoiceAuditWorkflowService {

    private final AuditTaskMapper auditTaskMapper;

    private final InvoiceInfoMapper invoiceInfoMapper;

    private final OcrService ocrService;

    private final AuditRuleService auditRuleService;

    private final AuditReportService auditReportService;

    public InvoiceAuditWorkflowServiceImpl(
            AuditTaskMapper auditTaskMapper,
            InvoiceInfoMapper invoiceInfoMapper,
            OcrService ocrService,
            AuditRuleService auditRuleService,
            AuditReportService auditReportService) {

        this.auditTaskMapper = auditTaskMapper;
        this.invoiceInfoMapper = invoiceInfoMapper;
        this.ocrService = ocrService;
        this.auditRuleService = auditRuleService;
        this.auditReportService = auditReportService;
    }

    @Override
    @Transactional
    public UploadInvoiceResult execute(MultipartFile file) {

        validateUpload(file);

        AuditTaskState state = new AuditTaskState();

        try {

            /*
             ** Node 1：保存上传文件
             */
            saveInvoiceFile(file, state);

            /*
             ** Node 2：创建审核任务
             */
            createAuditTask(state);

            /*
             ** Node 3：OCR
             */
            executeOcr(state);

            /*
             ** Node 4：规则审核
             */
            executeAuditRules(state);

            /*
             ** Node 5：生成报告
             */
            generateAuditReport(state);

            /*
             ** Node 6：整理接口返回结果
             */
            return buildResult(state);

        } catch (Exception e) {

            /*
             ** 数据库操作由 @Transactional 控制。
             **
             ** 但是注意：
             ** MySQL 可以自动回滚，
             ** 本地 uploads 文件无法跟随数据库事务自动回滚。
             **
             ** 后面可以增加文件补偿机制。
             */
            throw new RuntimeException(
                    "发票审核流程执行失败：" + e.getMessage(),
                    e
            );
        }
    }

    /*
     ** 检查上传文件
     */
    private void validateUpload(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }
    }

    /*
     ** Node 1：
     ** 保存原始发票文件
     */
    private void saveInvoiceFile(
            MultipartFile file,
            AuditTaskState state) throws Exception {

        String originalFilename = file.getOriginalFilename();

        String suffix = getFileSuffix(originalFilename);

        String datePath = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        String fileName = UUID.randomUUID() + suffix;

        String baseDir =
                System.getProperty("user.dir")
                        + File.separator
                        + "uploads"
                        + File.separator
                        + "invoices";

        File targetDir = new File(baseDir, datePath);

        if (!targetDir.exists()) {

            boolean created = targetDir.mkdirs();

            if (!created) {
                throw new RuntimeException("创建上传目录失败");
            }
        }

        File targetFile = new File(targetDir, fileName);

        file.transferTo(targetFile);

        state.setTargetFile(targetFile);

        state.completeStep("FILE_SAVED");
    }

    /*
     ** Node 2：
     ** 创建 audit_task
     */
    private void createAuditTask(AuditTaskState state) {

        String taskNo =
                "TASK-" + System.currentTimeMillis();

        AuditTask auditTask = new AuditTask();

        auditTask.setTaskNo(taskNo);

        auditTask.setUserId(null);

        auditTask.setStatus(
                AuditTaskStatus.UPLOADED
        );

        auditTask.setOriginalFilePath(
                state.getTargetFile()
                        .getAbsolutePath()
        );

        auditTask.setOcrRawText(null);

        auditTask.setFinalDecision(null);

        auditTask.setNeedHumanReview(false);

        auditTask.setReportPath(null);

        auditTaskMapper.insertAuditTask(auditTask);

        state.setAuditTask(auditTask);

        state.completeStep("TASK_CREATED");
    }

    /*
     ** Node 3：
     ** OCR → invoice_info
     */
    private void executeOcr(AuditTaskState state) {

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

        invoiceInfoMapper
                .insertInvoiceInfo(invoiceInfo);

        auditTaskMapper.updateTaskAfterOcr(
                auditTask.getId(),
                AuditTaskStatus.OCR_DONE,
                invoiceInfo.getRawJson()
        );

        auditTask.setStatus(
                AuditTaskStatus.OCR_DONE
        );

        auditTask.setOcrRawText(
                invoiceInfo.getRawJson()
        );

        state.setInvoiceInfo(invoiceInfo);

        state.completeStep("OCR_DONE");
    }

    /*
     ** Node 4：
     ** 执行审核规则
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

        auditTaskMapper.updateAuditResult(
                auditTask.getId(),
                AuditTaskStatus.AUDIT_DONE,
                auditResult.getFinalDecision(),
                auditResult.getNeedHumanReview()
        );

        auditTask.setStatus(
                AuditTaskStatus.AUDIT_DONE
        );

        auditTask.setFinalDecision(
                auditResult.getFinalDecision()
        );

        auditTask.setNeedHumanReview(
                auditResult.getNeedHumanReview()
        );

        state.setAuditResult(auditResult);

        state.completeStep("AUDIT_DONE");
    }

    /*
     ** Node 5：
     ** 生成审核报告
     */
    private void generateAuditReport(
            AuditTaskState state) {

        AuditReportResult auditReport =
                auditReportService.generateReport(
                        state.getAuditTask(),
                        state.getInvoiceInfo(),
                        state.getAuditResult()
                );

        auditTaskMapper.updateReportPath(
                state.getAuditTask().getId(),
                auditReport.getReportPath()
        );

        state.getAuditTask()
                .setReportPath(
                        auditReport.getReportPath()
                );

        state.setAuditReport(auditReport);

        state.completeStep("REPORT_GENERATED");
    }

    /*
     ** Node 6：
     ** 返回前端 VO
     */
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

    private String getFileSuffix(
            String originalFilename) {

        if (originalFilename == null
                || !originalFilename.contains(".")) {
            return "";
        }

        return originalFilename.substring(
                originalFilename.lastIndexOf(".")
        );
    }
}