package invoice_agent_backend.service.impl;

import invoice_agent_backend.entity.AuditRuleHit;
import invoice_agent_backend.entity.AuditTask;
import invoice_agent_backend.entity.InvoiceInfo;
import invoice_agent_backend.service.AuditReportService;
import invoice_agent_backend.vo.AuditReportResult;
import invoice_agent_backend.vo.AuditResult;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/*
 ** 审核报告生成服务。
 **
 ** 当前这个类可以先理解成 Report Agent 的最初版本。
 **
 ** 它不负责 OCR，也不负责审核规则判断。
 ** 它只负责把前面几个步骤的结果整理成一份人能看懂的审核报告。
 **
 ** 当前先生成 txt 文件：
 ** - 简单
 ** - 稳定
 ** - 方便调试
 **
 ** 后面如果要升级成 PDF，只需要替换这里的文件生成逻辑。
 */
@Service
public class AuditReportServiceImpl implements AuditReportService {

    @Override
    public AuditReportResult generateReport(AuditTask auditTask,
                                            InvoiceInfo invoiceInfo,
                                            AuditResult auditResult) {
        if (auditTask == null || auditTask.getId() == null) {
            throw new RuntimeException("审核任务不能为空");
        }

        if (invoiceInfo == null) {
            throw new RuntimeException("发票信息不能为空");
        }

        if (auditResult == null) {
            throw new RuntimeException("审核结果不能为空");
        }

        try {
            /*
             ** 报告文件也按日期分目录保存。
             **
             ** 例如：
             ** uploads/reports/20260708/AUDIT_REPORT_1.txt
             **
             ** 这样做的好处是：
             ** - 不会把所有报告都堆在一个文件夹里
             ** - 后面排查问题时，可以按日期快速定位
             */
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

            Path reportDir = Path.of(
                    System.getProperty("user.dir"),
                    "uploads",
                    "reports",
                    datePath
            );

            Files.createDirectories(reportDir);

            String reportFileName = "AUDIT_REPORT_" + auditTask.getId() + ".txt";
            Path reportFilePath = reportDir.resolve(reportFileName);

            String reportContent = buildReportContent(auditTask, invoiceInfo, auditResult);

            /*
             ** 真正把报告内容写入 txt 文件。
             ** 这里使用 UTF-8，避免中文报告内容乱码。
             */
            Files.writeString(reportFilePath, reportContent, StandardCharsets.UTF_8);

            return new AuditReportResult(
                    auditTask.getId(),
                    auditTask.getTaskNo(),
                    auditResult.getFinalDecision(),
                    auditResult.getNeedHumanReview(),
                    reportFilePath.toAbsolutePath().toString(),
                    reportContent
            );
        } catch (IOException e) {
            throw new RuntimeException("生成审核报告失败：" + e.getMessage(), e);
        }
    }

    @Override
    public String readReportContent(String reportPath) {
        if (isBlank(reportPath)) {
            throw new RuntimeException("报告路径不能为空");
        }

        try {
            Path path = Path.of(reportPath);

            if (!Files.exists(path)) {
                throw new RuntimeException("审核报告文件不存在");
            }

            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("读取审核报告失败：" + e.getMessage(), e);
        }
    }

    /*
     ** 生成报告正文。
     **
     ** 这里的内容现在先写得直白一点。
     ** 后面接入 LLM Agent 后，可以把这些结构化数据交给大模型，
     ** 让它生成更自然的审核意见。
     */
    private String buildReportContent(AuditTask auditTask,
                                      InvoiceInfo invoiceInfo,
                                      AuditResult auditResult) {
        StringBuilder builder = new StringBuilder();

        builder.append("智能发票报销审核报告").append("\n");
        builder.append("================================").append("\n\n");

        builder.append("一、任务信息").append("\n");
        builder.append("任务ID：").append(valueOf(auditTask.getId())).append("\n");
        builder.append("任务编号：").append(valueOf(auditTask.getTaskNo())).append("\n");
        builder.append("任务状态：")
                .append(valueOf(auditTask.getStatus()))
                .append("\n");
        builder.append("是否需要人工复核：")
                .append(Boolean.TRUE.equals(auditResult.getNeedHumanReview()) ? "是" : "否")
                .append("\n");
        builder.append("最终审核结论：").append(valueOf(auditResult.getFinalDecision())).append("\n\n");

        builder.append("二、发票识别信息").append("\n");
        builder.append("发票号码：").append(valueOf(invoiceInfo.getInvoiceNo())).append("\n");
        builder.append("发票日期：").append(valueOf(invoiceInfo.getInvoiceDate())).append("\n");
        builder.append("购买方名称：").append(valueOf(invoiceInfo.getBuyerName())).append("\n");
        builder.append("销售方名称：").append(valueOf(invoiceInfo.getSellerName())).append("\n");
        builder.append("发票类型：").append(valueOf(invoiceInfo.getInvoiceType())).append("\n");
        builder.append("发票金额：").append(moneyOf(invoiceInfo.getAmount())).append("\n");
        builder.append("税额：").append(moneyOf(invoiceInfo.getTaxAmount())).append("\n\n");

        builder.append("三、规则审核结果").append("\n");

        List<AuditRuleHit> ruleHits = auditResult.getRuleHits();

        if (ruleHits == null || ruleHits.isEmpty()) {
            builder.append("未命中风险规则，系统自动通过。").append("\n\n");
        } else {
            builder.append("本次审核命中 ").append(ruleHits.size()).append(" 条规则：").append("\n");

            for (int i = 0; i < ruleHits.size(); i++) {
                AuditRuleHit ruleHit = ruleHits.get(i);

                builder.append(i + 1).append(". ")
                        .append(valueOf(ruleHit.getRuleName()))
                        .append("（")
                        .append(valueOf(ruleHit.getRuleCode()))
                        .append("）")
                        .append("\n");

                builder.append("   命中结果：").append(valueOf(ruleHit.getHitResult())).append("\n");
                builder.append("   审核说明：").append(valueOf(ruleHit.getRuleMessage())).append("\n");
            }

            builder.append("\n");
        }

        builder.append("四、系统建议").append("\n");

        String finalDecision =
                auditResult.getFinalDecision();

        if ("REJECTED_BY_HUMAN"
                .equals(finalDecision)) {

            builder.append(
                    "该发票已经完成人工复核，人工审核结论为拒绝。"
            ).append("\n");

        } else if ("APPROVED_BY_HUMAN"
                .equals(finalDecision)) {

            builder.append(
                    "该发票已经完成人工复核，人工审核结论为通过。"
            ).append("\n");

        } else if (Boolean.TRUE.equals(
                auditResult.getNeedHumanReview())) {

            builder.append(
                    "该发票存在需要关注的风险点，建议进入人工复核流程。"
            ).append("\n");

        } else {

            builder.append(
                    "该发票暂未发现明显风险，可以进入自动通过流程。"
            ).append("\n");
        }

        return builder.toString();
    }

    private String valueOf(Object value) {
        return value == null ? "无" : String.valueOf(value);
    }

    private String moneyOf(BigDecimal value) {
        return value == null ? "无" : value.toPlainString();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}