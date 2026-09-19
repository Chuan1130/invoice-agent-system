package invoice_agent_backend.agent.supervisor.impl;

import invoice_agent_backend.agent.model.SupervisorAgentResponse;
import invoice_agent_backend.agent.supervisor.SupervisorAgentService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/*
 ** Agent 默认关闭时仍然保留 Controller，
 ** 这样接口会返回清晰的配置提示，而不是启动阶段直接失败。
 */
@Service
@ConditionalOnProperty(
        name = "agent.supervisor.enabled",
        havingValue = "false",
        matchIfMissing = true
)
public class DisabledSupervisorAgentService
        implements SupervisorAgentService {

    @Override
    public SupervisorAgentResponse ask(
            String message) {

        throw new RuntimeException(
                "Supervisor Agent 未启用。请设置 AGENT_SUPERVISOR_ENABLED=true、SPRING_AI_MODEL_CHAT=openai，并配置 LLM_API_KEY"
        );
    }
}
