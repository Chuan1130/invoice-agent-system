package invoice_agent_backend.mapper;

import invoice_agent_backend.agent.action.AgentActionRequestRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface AgentActionRequestMapper {

    int insertAction(
            AgentActionRequestRecord record
    );

    AgentActionRequestRecord selectByToken(
            @Param("actionToken") String actionToken
    );

    AgentActionRequestRecord selectByTokenForUpdate(
            @Param("actionToken") String actionToken
    );

    AgentActionRequestRecord selectByDedupeKey(
            @Param("dedupeKey") String dedupeKey
    );

    int updateExecuted(
            @Param("id") Long id,
            @Param("confirmedBy") String confirmedBy,
            @Param("resultSummary") String resultSummary,
            @Param("executedAt") LocalDateTime executedAt
    );

    int updateStatusIfPending(
            @Param("id") Long id,
            @Param("status") String status
    );

    int cancelPendingByRequestId(
            @Param("requestId") String requestId
    );
}
