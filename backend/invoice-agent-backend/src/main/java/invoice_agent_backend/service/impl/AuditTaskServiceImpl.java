package invoice_agent_backend.service.impl;

import invoice_agent_backend.entity.AuditRuleHit;
import invoice_agent_backend.entity.AuditTask;
import invoice_agent_backend.entity.InvoiceInfo;
import invoice_agent_backend.mapper.AuditRuleHitMapper;
import invoice_agent_backend.mapper.AuditTaskMapper;
import invoice_agent_backend.mapper.InvoiceInfoMapper;
import invoice_agent_backend.service.AuditRuleService;
import invoice_agent_backend.service.AuditTaskService;
import invoice_agent_backend.service.OcrService;
import invoice_agent_backend.vo.AuditResult;
import invoice_agent_backend.vo.UploadInvoiceResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;


// @Service 表示 Spring 会把它注册成业务组件
// Controller 需要 AuditTaskService 时，Spring 就会把这个实现类注入进去。
@Service
public class AuditTaskServiceImpl implements AuditTaskService {

    private final AuditTaskMapper auditTaskMapper;          // 负责操作 audit_task 表
    private final InvoiceInfoMapper invoiceInfoMapper;      // 负责操作 invoice_info 表
    private final AuditRuleHitMapper auditRuleHitMapper;    // 负责操作 audit_rule_hit 表
    private final OcrService ocrService;                    // 负责 OCR 识别；现在实际实现是假 OCR
    private final AuditRuleService auditRuleService;        // 负责审核规则校验

    public AuditTaskServiceImpl(AuditTaskMapper auditTaskMapper,
                                InvoiceInfoMapper invoiceInfoMapper,
                                AuditRuleHitMapper auditRuleHitMapper,
                                OcrService ocrService,
                                AuditRuleService auditRuleService) {
        this.auditTaskMapper = auditTaskMapper;
        this.invoiceInfoMapper = invoiceInfoMapper;
        this.auditRuleHitMapper = auditRuleHitMapper;
        this.ocrService = ocrService;
        this.auditRuleService = auditRuleService;
    }

    @Override
    @Transactional
    public UploadInvoiceResult uploadInvoice(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }

        try {
            /*
             ** 比如用户上传的是：invoice_sample.png 那么：
             ** originalFilename = invoice_sample.png；suffix = .png
             */
            String originalFilename = file.getOriginalFilename();
            String suffix = getFileSuffix(originalFilename);

            // 生成日期目录，再生成 UUID 文件名，因为如果每个人都上传 invoice.png，文件名会冲突。UUID 可以避免覆盖。
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String fileName = UUID.randomUUID() + suffix;

            String baseDir = System.getProperty("user.dir")
                    + File.separator + "uploads"
                    + File.separator + "invoices";

            // 如果今天的发票文件夹还不存在，就创建一个。
            File targetDir = new File(baseDir, datePath);
            if (!targetDir.exists()) {
                boolean created = targetDir.mkdirs();
                if (!created) {
                    throw new RuntimeException("创建上传目录失败");
                }
            }

            /*
             ** 数据库不适合直接存大图片。现在的设计是：
             ** 图片本体：存在 uploads 文件夹
             ** 图片路径：存在 MySQL 的 audit_task.original_file_path 字段
             */
            File targetFile = new File(targetDir, fileName);
            file.transferTo(targetFile);  // 暂时把图片存到 upload 目录里，在接入真实 OCR 之前。

            // 给这次上传创建一个任务编号，之后可以追踪这张发票处理到哪一步。
            String taskNo = "TASK-" + System.currentTimeMillis();

            /*
             ** 创建 AuditTask 对象，将“文件路径”存入数据库对应表。
             ** 这些字段对应数据库里的 audit_task 表。
             ** 业务上它表示“一次发票审核任务”。
             */
            AuditTask auditTask = new AuditTask();
            auditTask.setTaskNo(taskNo);
            auditTask.setUserId(null);
            auditTask.setStatus("UPLOADED");
            auditTask.setOriginalFilePath(targetFile.getAbsolutePath());
            auditTask.setOcrRawText(null);
            auditTask.setFinalDecision(null);
            auditTask.setNeedHumanReview(false);
            auditTask.setReportPath(null);

            // 存入 audit_task 表。插入成功后，MySQL 自动生成的 id 会回填到 auditTask.id。
            auditTaskMapper.insertAuditTask(auditTask);

            // 调用 OCR 服务去根据 id 和图片路径处理图片，然后返回并储存进 invoiceInfo 对象。
            // 当前用的是 MockOcrServiceImpl，不是真实 OCR。
            InvoiceInfo invoiceInfo = ocrService.recognizeInvoice(
                    auditTask.getId(),
                    targetFile.getAbsolutePath()
            );

            // 识别出的发票信息落库，保存到 invoice_info 表。
            invoiceInfoMapper.insertInvoiceInfo(invoiceInfo);

            // OCR 完成后，先更新任务状态为 OCR_DONE，并保存 OCR 原始 JSON。
            auditTaskMapper.updateTaskAfterOcr(
                    auditTask.getId(),
                    "OCR_DONE",
                    invoiceInfo.getRawJson()
            );

            /*
             ** 新增流程：执行审核规则。
             **
             ** 当前规则包括：
             ** 1. 必要字段不能为空
             ** 2. 金额超过 50000 需要人工复核
             ** 3. 发票号重复需要人工复核
             **
             ** checkRules 会：
             ** - 判断是否命中规则
             ** - 把命中的规则保存到 audit_rule_hit 表
             ** - 返回最终审核结果 AuditResult
             */
            AuditResult auditResult = auditRuleService.checkRules(invoiceInfo);

            /*
             ** 审核完成后，把最终结论更新回 audit_task 表。
             **
             ** status = AUDIT_DONE
             ** final_decision = APPROVED / NEED_HUMAN_REVIEW
             ** need_human_review = true / false
             */
            auditTaskMapper.updateAuditResult(
                    auditTask.getId(),
                    "AUDIT_DONE",
                    auditResult.getFinalDecision(),
                    auditResult.getNeedHumanReview()
            );

            auditTask.setStatus("AUDIT_DONE");
            auditTask.setFinalDecision(auditResult.getFinalDecision());
            auditTask.setNeedHumanReview(auditResult.getNeedHumanReview());

            /*
             ** 这个对象是专门返回给前端的 VO，也就是 View Object。
             ** 最终前端拿到的是 JSON 格式的结果：
             ** - taskId
             ** - taskNo
             ** - status
             ** - originalFilePath
             ** - invoiceInfo
             ** - auditResult
             */
            return new UploadInvoiceResult(
                    auditTask.getId(),
                    auditTask.getTaskNo(),
                    auditTask.getStatus(),
                    auditTask.getOriginalFilePath(),
                    invoiceInfo,
                    auditResult
            );
        } catch (Exception e) {
            throw new RuntimeException("上传发票失败：" + e.getMessage(), e);
        }
    }

    @Override
    public AuditTask getTaskById(Long id) {
        if (id == null) {
            throw new RuntimeException("任务ID不能为空");
        }

        AuditTask auditTask = auditTaskMapper.selectAuditTaskById(id);

        if (auditTask == null) {
            throw new RuntimeException("任务不存在");
        }

        return auditTask;
    }

    @Override
    public InvoiceInfo getInvoiceInfoByTaskId(Long taskId) {
        if (taskId == null) {
            throw new RuntimeException("任务ID不能为空");
        }

        InvoiceInfo invoiceInfo = invoiceInfoMapper.selectInvoiceInfoByTaskId(taskId);

        if (invoiceInfo == null) {
            throw new RuntimeException("发票信息不存在");
        }

        return invoiceInfo;
    }

    @Override
    public List<AuditRuleHit> getRuleHitsByTaskId(Long taskId) {
        if (taskId == null) {
            throw new RuntimeException("任务ID不能为空");
        }

        return auditRuleHitMapper.selectRuleHitsByTaskId(taskId);
    }

    private String getFileSuffix(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf("."));
    }
}