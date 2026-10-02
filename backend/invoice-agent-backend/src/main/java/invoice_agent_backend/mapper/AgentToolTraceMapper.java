package invoice_agent_backend.mapper;

import invoice_agent_backend.agent.trace.AgentToolTrace;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AgentToolTraceMapper {

    int insertAgentToolTrace(AgentToolTrace trace);

    List<AgentToolTrace> selectByRequestId(String requestId);
}
