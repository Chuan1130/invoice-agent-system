package invoice_agent_backend.mapper;

import invoice_agent_backend.entity.AuditRuleHit;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/*
 ** 负责操作 audit_rule_hit 表。
 **
 ** insertAuditRuleHit:
 ** 保存一条规则命中记录。
 **
 ** selectRuleHitsByTaskId:
 ** 根据任务ID查询这个任务命中过哪些规则。
 */
@Mapper
public interface AuditRuleHitMapper {

    int insertAuditRuleHit(AuditRuleHit auditRuleHit);

    List<AuditRuleHit> selectRuleHitsByTaskId(Long taskId);
}