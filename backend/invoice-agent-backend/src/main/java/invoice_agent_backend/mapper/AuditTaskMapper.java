package invoice_agent_backend.mapper;

import invoice_agent_backend.entity.AuditTask;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditTaskMapper {

    int insertAuditTask(AuditTask auditTask);

    AuditTask selectAuditTaskById(Long id);
}