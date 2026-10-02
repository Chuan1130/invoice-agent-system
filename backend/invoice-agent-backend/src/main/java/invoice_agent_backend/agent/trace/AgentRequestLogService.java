package invoice_agent_backend.agent.trace;

import invoice_agent_backend.agent.model.AgentRequestTraceResult;

public interface AgentRequestLogService {

    void start(String requestId, String userMessage);

    void complete(String requestId, String answer);

    void fail(String requestId, String errorMessage);

    AgentRequestTraceResult getRequestTrace(String requestId);
}
