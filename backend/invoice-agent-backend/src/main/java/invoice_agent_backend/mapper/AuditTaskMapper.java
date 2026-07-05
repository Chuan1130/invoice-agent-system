package invoice_agent_backend.mapper;

import invoice_agent_backend.entity.AuditTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AuditTaskMapper {

    int insertAuditTask(AuditTask auditTask);

    AuditTask selectAuditTaskById(Long id);

    int updateTaskAfterOcr(@Param("id") Long id,
                           @Param("status") String status,
                           @Param("ocrRawText") String ocrRawText);

    /*
     ** OCR 完成后，继续执行规则审核。
     ** 规则审核完成后，需要把最终审核结论更新回 audit_task 表。
     */
    int updateAuditResult(@Param("id") Long id,
                          @Param("status") String status,
                          @Param("finalDecision") String finalDecision,
                          @Param("needHumanReview") Boolean needHumanReview);
}