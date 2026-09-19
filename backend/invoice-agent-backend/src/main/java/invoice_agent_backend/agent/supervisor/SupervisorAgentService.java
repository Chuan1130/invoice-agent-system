package invoice_agent_backend.agent.supervisor;

import invoice_agent_backend.agent.model.SupervisorAgentResponse;

/*
 ** Supervisor Agent 对外服务接口。
 */
public interface SupervisorAgentService {

    SupervisorAgentResponse ask(String message);
}
