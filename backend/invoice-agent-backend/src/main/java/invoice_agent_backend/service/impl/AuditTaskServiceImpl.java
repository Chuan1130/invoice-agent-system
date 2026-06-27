package invoice_agent_backend.service.impl;

import invoice_agent_backend.entity.AuditTask;
import invoice_agent_backend.mapper.AuditTaskMapper;
import invoice_agent_backend.service.AuditTaskService;
import invoice_agent_backend.vo.UploadInvoiceResult;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class AuditTaskServiceImpl implements AuditTaskService {

    private final AuditTaskMapper auditTaskMapper;

    public AuditTaskServiceImpl(AuditTaskMapper auditTaskMapper) {
        this.auditTaskMapper = auditTaskMapper;
    }

    @Override
    public UploadInvoiceResult uploadInvoice(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String suffix = getFileSuffix(originalFilename);

            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String fileName = UUID.randomUUID() + suffix;

            String baseDir = System.getProperty("user.dir") + File.separator + "uploads" + File.separator + "invoices";
            File targetDir = new File(baseDir, datePath);

            if (!targetDir.exists()) {
                boolean created = targetDir.mkdirs();
                if (!created) {
                    throw new RuntimeException("创建上传目录失败");
                }
            }

            File targetFile = new File(targetDir, fileName);
            file.transferTo(targetFile);  // 暂时存到upload目录里，在接入OCR之前

            String taskNo = "TASK-" + System.currentTimeMillis();

            // 创建AuditTask对象，将文件路径存入数据库对应表，
            AuditTask auditTask = new AuditTask();
            auditTask.setTaskNo(taskNo);
            auditTask.setUserId(null);
            auditTask.setStatus("UPLOADED");
            auditTask.setOriginalFilePath(targetFile.getAbsolutePath());
            auditTask.setOcrRawText(null);
            auditTask.setFinalDecision(null);
            auditTask.setNeedHumanReview(false);
            auditTask.setReportPath(null);

            // 存入数据库
            auditTaskMapper.insertAuditTask(auditTask);

            return new UploadInvoiceResult(
                    auditTask.getId(),
                    auditTask.getTaskNo(),
                    auditTask.getStatus(),
                    auditTask.getOriginalFilePath()
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

    private String getFileSuffix(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf("."));
    }
}