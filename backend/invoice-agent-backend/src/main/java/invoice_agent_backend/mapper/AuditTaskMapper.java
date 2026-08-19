package invoice_agent_backend.mapper;

import invoice_agent_backend.entity.AuditTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AuditTaskMapper {

    int insertAuditTask(AuditTask auditTask);

    AuditTask selectAuditTaskById(Long id);

    /*
     ** 通用任务状态更新。
     **
     ** 只有数据库中的状态仍然等于 currentStatus 时，
     ** 才能更新成 targetStatus。
     */
    int updateTaskStatus(
            @Param("id") Long id,
            @Param("currentStatus") String currentStatus,
            @Param("targetStatus") String targetStatus
    );

    /*
     ** OCR 完成后只更新 OCR 原文。
     **
     ** 状态更新统一交给 Lifecycle Service。
     */
    int updateTaskAfterOcr(
            @Param("id") Long id,
            @Param("ocrRawText") String ocrRawText
    );

    /*
     ** 规则审核完成后只更新审核结论。
     **
     ** 状态更新统一交给 Lifecycle Service。
     */
    int updateAuditResult(
            @Param("id") Long id,
            @Param("finalDecision") String finalDecision,
            @Param("needHumanReview") Boolean needHumanReview
    );

    int updateReportPath(
            @Param("id") Long id,
            @Param("reportPath") String reportPath
    );

    List<AuditTask> selectAuditTaskPage(
            @Param("status") String status,
            @Param("offset") Integer offset,
            @Param("size") Integer size
    );

    long countAuditTasks(
            @Param("status") String status
    );

    /*
     ** 人工审核完成后只更新最终审核结论。
     **
     ** AUDIT_DONE -> COMPLETED
     ** 由 Lifecycle Service 完成。
     */
    int updateHumanReviewResult(
            @Param("id") Long id,
            @Param("finalDecision") String finalDecision,
            @Param("needHumanReview") Boolean needHumanReview
    );
}