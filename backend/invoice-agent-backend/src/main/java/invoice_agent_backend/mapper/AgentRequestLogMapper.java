package invoice_agent_backend.mapper;

import invoice_agent_backend.agent.trace.AgentRequestRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface AgentRequestLogMapper {

    int insertAgentRequest(AgentRequestRecord record);

    int updateCompleted(
            @Param("requestId") String requestId,
            @Param("answer") String answer,
            @Param("completedAt") LocalDateTime completedAt);

    int updateFailed(
            @Param("requestId") String requestId,
            @Param("errorMessage") String errorMessage,
            @Param("completedAt") LocalDateTime completedAt);

    AgentRequestRecord selectByRequestId(String requestId);
}
