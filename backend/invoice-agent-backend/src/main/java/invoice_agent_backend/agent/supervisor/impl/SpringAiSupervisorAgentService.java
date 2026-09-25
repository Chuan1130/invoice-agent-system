package invoice_agent_backend.agent.supervisor.impl;

import invoice_agent_backend.agent.model.SupervisorAgentResponse;
import invoice_agent_backend.agent.supervisor.SupervisorAgentService;
import invoice_agent_backend.agent.tool.InvoiceAuditAgentTools;
import invoice_agent_backend.agent.trace.AgentRequestLogService;
import invoice_agent_backend.agent.trace.AgentToolTrace;
import invoice_agent_backend.agent.trace.AgentToolTraceContext;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/*
 ** Supervisor Agent。
 **
 ** 模型负责理解问题、选择 Tool 和组织答案；
 ** 发票事实仍然来自现有 Service，状态迁移仍然由原 Workflow 管理。
 **
 ** 每次请求会生成 requestId，并记录整次 Agent 请求生命周期：
 ** RUNNING -> COMPLETED / FAILED。
 */
@Service
@ConditionalOnProperty(
        name = "agent.supervisor.enabled",
        havingValue = "true"
)
public class SpringAiSupervisorAgentService
        implements SupervisorAgentService {

    private static final String SYSTEM_PROMPT = """
            你是智能发票报销审核系统的 Supervisor Agent。

            你的职责是理解用户的审核问题，选择合适的业务 Tool，
            再根据 Tool 返回的真实数据给出清晰结论。

            必须遵守以下规则：
            1. 不得编造任务、发票、规则命中或审核结论。
            2. 只要问题涉及数据库中的具体任务事实，就必须调用 Tool。
            3. 当前开放的 Tool 全部是只读能力，不要声称已经修改任务状态。
            4. 不得绕过现有状态机，也不得替用户执行人工 APPROVE 或 REJECT。
            5. 用户没有提供足够的任务 ID 时，先说明缺少什么信息。
            6. 区分系统确定性结论与模型解释。最终状态以 Tool 返回数据为准。

            回答尽量按下面结构组织：
            【意图】
            【执行计划】
            【工具结果】
            【结论】
            【下一步】
            """;

    private final ChatClient chatClient;
    private final AgentToolTraceContext traceContext;
    private final AgentRequestLogService requestLogService;

    public SpringAiSupervisorAgentService(
            ChatClient.Builder chatClientBuilder,
            InvoiceAuditAgentTools invoiceAuditAgentTools,
            AgentToolTraceContext traceContext,
            AgentRequestLogService requestLogService) {

        this.chatClient =
                chatClientBuilder
                        .defaultSystem(SYSTEM_PROMPT)
                        .defaultTools(
                                invoiceAuditAgentTools
                        )
                        .build();

        this.traceContext = traceContext;
        this.requestLogService = requestLogService;
    }

    @Override
    public SupervisorAgentResponse ask(
            String message) {

        if (message == null
                || message.trim().isEmpty()) {

            throw new RuntimeException(
                    "Supervisor message 不能为空"
            );
        }

        String safeMessage = message.trim();
        String requestId = newRequestId();

        traceContext.start(requestId);
        requestLogService.start(
                requestId,
                safeMessage
        );

        try {
            String answer =
                    chatClient
                            .prompt()
                            .user(safeMessage)
                            .call()
                            .content();

            List<AgentToolTrace> toolTraces =
                    traceContext.snapshot();

            requestLogService.complete(
                    requestId,
                    answer
            );

            return new SupervisorAgentResponse(
                    requestId,
                    answer,
                    toolTraces
            );

        } catch (RuntimeException e) {

            requestLogService.fail(
                    requestId,
                    e.getMessage()
            );

            throw e;

        } finally {
            traceContext.clear();
        }
    }

    private String newRequestId() {
        return "AGT-" + UUID.randomUUID();
    }
}
