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
}