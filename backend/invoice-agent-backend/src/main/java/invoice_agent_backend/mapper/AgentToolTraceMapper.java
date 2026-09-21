package invoice_agent_backend.mapper;

import invoice_agent_backend.agent.trace.AgentToolTrace;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

public interface AgentToolTraceMapper {
    @Insert("""
            INSERT INTO agent_tool_trace
                (request_id, sequence_no, tool_name, input_summary, result_summary, success, called_at)
            VALUES
                (#{requestId}, #{sequenceNo}, #{trace.toolName}, #{trace.inputSummary},
                 #{trace.resultSummary}, #{trace.success}, #{trace.calledAt})
            """)
    int insert(@Param("requestId") String requestId,
               @Param("sequenceNo") int sequenceNo,
               @Param("trace") AgentToolTrace trace);
}
