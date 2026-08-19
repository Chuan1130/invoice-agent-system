package invoice_agent_backend.workflow.impl;

import invoice_agent_backend.constant.AuditTaskStatus;
import invoice_agent_backend.entity.AuditTask;
import invoice_agent_backend.service.AuditTaskLifecycleService;
import invoice_agent_backend.vo.UploadInvoiceResult;
import invoice_agent_backend.workflow.InvoiceAuditWorkflowCoreService;
import invoice_agent_backend.workflow.InvoiceAuditWorkflowService;
import invoice_agent_backend.workflow.state.AuditTaskState;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/*
 ** 发票审核外层 Orchestrator。
 **
 ** 注意：
 ** 这个类不添加 @Transactional。
 **
 ** 它负责：
 **
 ** 1. 校验上传文件
 ** 2. 保存本地文件
 ** 3. 独立事务创建任务
 ** 4. 独立事务提交 OCR_PROCESSING
 ** 5. 调用 Core 主事务
 ** 6. 捕获异常并独立提交 FAILED
 */
@Service
public class InvoiceAuditWorkflowServiceImpl
        implements InvoiceAuditWorkflowService {

    private final AuditTaskLifecycleService
            auditTaskLifecycleService;

    private final InvoiceAuditWorkflowCoreService
            workflowCoreService;

    public InvoiceAuditWorkflowServiceImpl(
            AuditTaskLifecycleService auditTaskLifecycleService,
            InvoiceAuditWorkflowCoreService workflowCoreService) {

        this.auditTaskLifecycleService =
                auditTaskLifecycleService;

        this.workflowCoreService =
                workflowCoreService;
    }

    /*
     ** 外层方法故意不添加 @Transactional。
     */
    @Override
    public UploadInvoiceResult execute(
            MultipartFile file) {

        validateUpload(file);

        AuditTaskState state =
                new AuditTaskState();

        AuditTask auditTask = null;

        try {

            /*
             ** Node 1：
             ** 保存上传文件。
             **
             ** 本地文件系统不受数据库事务控制。
             */
            saveInvoiceFile(
                    file,
                    state
            );

            /*
             ** Node 2：
             ** 使用 REQUIRES_NEW 创建任务。
             **
             ** 创建成功后立即提交 UPLOADED。
             */
            auditTask =
                    auditTaskLifecycleService
                            .createTask(
                                    state.getTargetFile()
                                            .getAbsolutePath()
                            );

            state.setAuditTask(
                    auditTask
            );

            state.completeStep(
                    "TASK_CREATED"
            );

            /*
             ** Node 3：
             ** 使用 REQUIRES_NEW 更新为 OCR_PROCESSING。
             **
             ** 这个事务会在真正调用百度 OCR 前提交。
             */
            auditTaskLifecycleService
                    .markOcrProcessing(
                            auditTask.getId()
                    );

            /*
             ** 同步更新内存中的状态对象。
             */
            auditTask.setStatus(
                    AuditTaskStatus.OCR_PROCESSING
            );

            state.completeStep(
                    "OCR_PROCESSING"
            );

            /*
             ** Node 4：
             ** 进入 Core 主事务。
             */
            return workflowCoreService
                    .execute(state);

        } catch (Exception workflowException) {

            /*
             ** 只有任务确实创建成功以后，
             ** 才有任务可以更新为 FAILED。
             */
            if (auditTask != null
                    && auditTask.getId() != null) {

                try {

                    /*
                     ** REQUIRES_NEW：
                     **
                     ** Core 主事务已经回滚，
                     ** 这里重新开启独立事务记录 FAILED。
                     */
                    auditTaskLifecycleService
                            .markFailed(
                                    auditTask.getId()
                            );

                } catch (Exception failedStatusException) {

                    /*
                     ** 不覆盖最初的业务异常。
                     **
                     ** 将 FAILED 保存异常作为附加异常保留。
                     */
                    workflowException.addSuppressed(
                            failedStatusException
                    );
                }
            }

            String taskMessage =
                    auditTask == null
                            || auditTask.getId() == null
                            ? ""
                            : "，任务ID："
                            + auditTask.getId();

            throw new RuntimeException(
                    "发票审核流程执行失败"
                            + taskMessage
                            + "，原因："
                            + workflowException
                            .getMessage(),
                    workflowException
            );
        }
    }

    private void validateUpload(
            MultipartFile file) {

        if (file == null
                || file.isEmpty()) {

            throw new RuntimeException(
                    "上传文件不能为空"
            );
        }
    }

    /*
     ** 保存原始发票文件。
     **
     ** 这一部分保留原来的实现。
     */
    private void saveInvoiceFile(
            MultipartFile file,
            AuditTaskState state)
            throws Exception {

        String originalFilename =
                file.getOriginalFilename();

        String suffix =
                getFileSuffix(
                        originalFilename
                );

        String datePath =
                LocalDate.now()
                        .format(
                                DateTimeFormatter
                                        .ofPattern(
                                                "yyyyMMdd"
                                        )
                        );

        String fileName =
                UUID.randomUUID()
                        + suffix;

        String baseDir =
                System.getProperty(
                        "user.dir"
                )
                        + File.separator
                        + "uploads"
                        + File.separator
                        + "invoices";

        File targetDir =
                new File(
                        baseDir,
                        datePath
                );

        if (!targetDir.exists()) {

            boolean created =
                    targetDir.mkdirs();

            if (!created) {
                throw new RuntimeException(
                        "创建上传目录失败"
                );
            }
        }

        File targetFile =
                new File(
                        targetDir,
                        fileName
                );

        file.transferTo(
                targetFile
        );

        state.setTargetFile(
                targetFile
        );

        state.completeStep(
                "FILE_SAVED"
        );
    }

    private String getFileSuffix(
            String originalFilename) {

        if (originalFilename == null
                || !originalFilename
                .contains(".")) {

            return "";
        }

        return originalFilename.substring(
                originalFilename
                        .lastIndexOf(".")
        );
    }
}