package invoice_agent_backend.agent.tool;

import invoice_agent_backend.agent.trace.AgentToolTraceContext;
import invoice_agent_backend.common.PageResult;
import invoice_agent_backend.entity.AuditRuleHit;
import invoice_agent_backend.entity.AuditTask;
import invoice_agent_backend.entity.HumanReviewRecord;
import invoice_agent_backend.entity.InvoiceInfo;
import invoice_agent_backend.service.AuditTaskService;
import invoice_agent_backend.vo.AuditReportResult;
import invoice_agent_backend.vo.AuditTaskDetailResult;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/*
 ** 提供给 Supervisor Agent 的业务 Tool。
 **
 ** 第一版只开放只读能力。
 ** OCR、规则执行、状态迁移和人工审核仍然由原来的 Workflow 控制，
 ** 避免模型绕过状态机直接修改业务数据。
 */
@Component
public class InvoiceAuditAgentTools {

    private final AuditTaskService auditTaskService;

    private final AgentToolTraceContext traceContext;

    public InvoiceAuditAgentTools(
            AuditTaskService auditTaskService,
            AgentToolTraceContext traceContext) {

        this.auditTaskService = auditTaskService;
        this.traceContext = traceContext;
    }

    @Tool(
            name = "getTaskDetailTool",
            description = "查询指定发票审核任务的完整摘要，包括任务状态、最终结论、发票字段、规则命中、人工复核记录和报告是否存在。需要任务 ID。"
    )
    public String getTaskDetailTool(
            @ToolParam(description = "审核任务 ID")
            Long taskId) {

        String input = "taskId=" + taskId;

        try {
            AuditTaskDetailResult detail =
                    auditTaskService.getTaskDetail(taskId);

            String result =
                    buildTaskDetailSummary(detail);

            traceContext.recordSuccess(
                    "getTaskDetailTool",
                    input,
                    shortTaskSummary(detail.getAuditTask())
            );

            return result;

        } catch (RuntimeException e) {

            traceContext.recordFailure(
                    "getTaskDetailTool",
                    input,
                    e.getMessage()
            );

            throw e;
        }
    }

    @Tool(
            name = "getInvoiceInfoTool",
            description = "查询指定审核任务的结构化发票字段。适合回答发票号码、日期、金额、税额、购买方、销售方和发票类型。需要任务 ID。"
    )
    public String getInvoiceInfoTool(
            @ToolParam(description = "审核任务 ID")
            Long taskId) {

        String input = "taskId=" + taskId;

        try {
            InvoiceInfo invoiceInfo =
                    auditTaskService
                            .getInvoiceInfoByTaskId(taskId);

            String result =
                    buildInvoiceInfoSummary(invoiceInfo);

            traceContext.recordSuccess(
                    "getInvoiceInfoTool",
                    input,
                    "invoiceNo="
                            + valueOf(invoiceInfo.getInvoiceNo())
                            + ", amount="
                            + valueOf(invoiceInfo.getAmount())
            );

            return result;

        } catch (RuntimeException e) {

            traceContext.recordFailure(
                    "getInvoiceInfoTool",
                    input,
                    e.getMessage()
            );

            throw e;
        }
    }

    @Tool(
            name = "getRuleHitsTool",
            description = "查询指定审核任务实际命中的审核规则。适合解释为什么任务需要人工复核。需要任务 ID。"
    )
    public String getRuleHitsTool(
            @ToolParam(description = "审核任务 ID")
            Long taskId) {

        String input = "taskId=" + taskId;

        try {
            List<AuditRuleHit> ruleHits =
                    auditTaskService
                            .getRuleHitsByTaskId(taskId);

            String result =
                    buildRuleHitSummary(ruleHits);

            traceContext.recordSuccess(
                    "getRuleHitsTool",
                    input,
                    "ruleHitCount=" + ruleHits.size()
            );

            return result;

        } catch (RuntimeException e) {

            traceContext.recordFailure(
                    "getRuleHitsTool",
                    input,
                    e.getMessage()
            );

            throw e;
        }
    }

    @Tool(
            name = "getAuditReportTool",
            description = "读取指定审核任务已经生成的审核报告。适合查看系统最终形成的审核意见。需要任务 ID。"
    )
    public String getAuditReportTool(
            @ToolParam(description = "审核任务 ID")
            Long taskId) {

        String input = "taskId=" + taskId;

        try {
            AuditReportResult report =
                    auditTaskService
                            .getReportByTaskId(taskId);

            traceContext.recordSuccess(
                    "getAuditReportTool",
                    input,
                    "finalDecision="
                            + valueOf(report.getFinalDecision())
                            + ", reportLoaded=true"
            );

            return report.getReportContent();

        } catch (RuntimeException e) {

            traceContext.recordFailure(
                    "getAuditReportTool",
                    input,
                    e.getMessage()
            );

            throw e;
        }
    }

    @Tool(
            name = "listTasksByStatusTool",
            description = "查询最近的审核任务。status 传 ALL 表示全部，也可以传 UPLOADED、OCR_PROCESSING、OCR_DONE、AUDIT_DONE、COMPLETED 或 FAILED。"
    )
    public String listTasksByStatusTool(
            @ToolParam(description = "任务状态；传 ALL 查询全部最近任务")
            String status) {

        String safeStatus =
                status == null
                        ? "ALL"
                        : status.trim().toUpperCase();

        String queryStatus =
                "ALL".equals(safeStatus)
                        ? null
                        : safeStatus;

        String input = "status=" + safeStatus;

        try {
            PageResult<AuditTask> page =
                    auditTaskService.getTaskPage(
                            queryStatus,
                            1,
                            20
                    );

            String result =
                    buildTaskPageSummary(page);

            traceContext.recordSuccess(
                    "listTasksByStatusTool",
                    input,
                    "total=" + page.getTotal()
            );

            return result;

        } catch (RuntimeException e) {

            traceContext.recordFailure(
                    "listTasksByStatusTool",
                    input,
                    e.getMessage()
            );

            throw e;
        }
    }

    private String buildTaskDetailSummary(
            AuditTaskDetailResult detail) {

        StringBuilder builder =
                new StringBuilder();

        AuditTask task =
                detail.getAuditTask();

        builder.append("任务信息\n");
        builder.append(shortTaskSummary(task))
                .append("\n\n");

        builder.append("发票信息\n");

        if (detail.getInvoiceInfo() == null) {
            builder.append("暂无发票结构化信息\n");
        } else {
            builder.append(
                    buildInvoiceInfoSummary(
                            detail.getInvoiceInfo()
                    )
            ).append("\n");
        }

        builder.append("\n规则命中\n")
                .append(
                        buildRuleHitSummary(
                                detail.getRuleHits()
                        )
                );

        builder.append("\n人工复核记录数：")
                .append(
                        detail.getHumanReviews() == null
                                ? 0
                                : detail.getHumanReviews().size()
                )
                .append("\n");

        if (detail.getHumanReviews() != null) {
            for (HumanReviewRecord record
                    : detail.getHumanReviews()) {

                builder.append("- decision=")
                        .append(valueOf(record.getDecision()))
                        .append(", reviewer=")
                        .append(valueOf(record.getReviewer()))
                        .append(", comment=")
                        .append(valueOf(record.getReviewComment()))
                        .append("\n");
            }
        }

        builder.append("报告：")
                .append(
                        detail.getAuditReport() == null
                                ? "尚未生成"
                                : "已生成"
                );

        return builder.toString();
    }

    private String buildInvoiceInfoSummary(
            InvoiceInfo invoiceInfo) {

        return "invoiceNo="
                + valueOf(invoiceInfo.getInvoiceNo())
                + "\ninvoiceDate="
                + valueOf(invoiceInfo.getInvoiceDate())
                + "\namount="
                + valueOf(invoiceInfo.getAmount())
                + "\ntaxAmount="
                + valueOf(invoiceInfo.getTaxAmount())
                + "\nbuyerName="
                + valueOf(invoiceInfo.getBuyerName())
                + "\nsellerName="
                + valueOf(invoiceInfo.getSellerName())
                + "\ninvoiceType="
                + valueOf(invoiceInfo.getInvoiceType());
    }

    private String buildRuleHitSummary(
            List<AuditRuleHit> ruleHits) {

        if (ruleHits == null
                || ruleHits.isEmpty()) {
            return "未命中风险规则";
        }

        StringBuilder builder =
                new StringBuilder();

        for (AuditRuleHit ruleHit : ruleHits) {
            builder.append("- ")
                    .append(valueOf(ruleHit.getRuleCode()))
                    .append(" | ")
                    .append(valueOf(ruleHit.getRuleName()))
                    .append(" | ")
                    .append(valueOf(ruleHit.getRuleMessage()))
                    .append("\n");
        }

        return builder.toString().trim();
    }

    private String buildTaskPageSummary(
            PageResult<AuditTask> page) {

        StringBuilder builder =
                new StringBuilder();

        builder.append("total=")
                .append(page.getTotal())
                .append("\n");

        if (page.getRecords() == null
                || page.getRecords().isEmpty()) {

            builder.append("没有符合条件的任务");
            return builder.toString();
        }

        for (AuditTask task : page.getRecords()) {
            builder.append("- ")
                    .append(shortTaskSummary(task))
                    .append("\n");
        }

        return builder.toString().trim();
    }

    private String shortTaskSummary(
            AuditTask task) {

        if (task == null) {
            return "任务不存在";
        }

        return "taskId="
                + valueOf(task.getId())
                + ", taskNo="
                + valueOf(task.getTaskNo())
                + ", status="
                + valueOf(task.getStatus())
                + ", finalDecision="
                + valueOf(task.getFinalDecision())
                + ", needHumanReview="
                + valueOf(task.getNeedHumanReview());
    }

    private String valueOf(Object value) {
        return value == null
                ? "无"
                : String.valueOf(value);
    }
}
