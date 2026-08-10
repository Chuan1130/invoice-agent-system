package invoice_agent_backend.mapper;

import invoice_agent_backend.entity.AuditTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AuditTaskMapper {

    int insertAuditTask(AuditTask auditTask);

    AuditTask selectAuditTaskById(Long id);

    int updateTaskAfterOcr(
            @Param("id") Long id,
            @Param("status") String status,
            @Param("ocrRawText") String ocrRawText
    );

    int updateAuditResult(
            @Param("id") Long id,
            @Param("status") String status,
            @Param("finalDecision") String finalDecision,
            @Param("needHumanReview") Boolean needHumanReview
    );

    int updateReportPath(
            @Param("id") Long id,
            @Param("reportPath") String reportPath
    );

    /*
     ** 分页查询任务。
     **
     ** status 可以为空。
     ** 如果为空，就查询所有任务。
     */
    List<AuditTask> selectAuditTaskPage(
            @Param("status") String status,
            @Param("offset") Integer offset,
            @Param("size") Integer size
    );

    long countAuditTasks(
            @Param("status") String status
    );

    /*
     ** 人工审核完成以后，
     ** 更新 audit_task 的最终状态。
     */
    int updateHumanReviewResult(
            @Param("id") Long id,
            @Param("status") String status,
            @Param("finalDecision") String finalDecision,
            @Param("needHumanReview") Boolean needHumanReview
    );
}