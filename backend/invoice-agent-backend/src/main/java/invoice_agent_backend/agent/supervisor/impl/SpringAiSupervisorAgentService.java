package invoice_agent_backend.agent.supervisor.impl;

import invoice_agent_backend.agent.action.AgentActionContext;
import invoice_agent_backend.agent.action.AgentActionService;
import invoice_agent_backend.agent.model.AgentPendingActionView;
import invoice_agent_backend.agent.model.SupervisorAgentResponse;
import invoice_agent_backend.agent.model.SupervisorStructuredAnswer;
import invoice_agent_backend.agent.supervisor.SupervisorAgentService;
import invoice_agent_backend.agent.tool.InvoiceAuditAgentTools;
import invoice_agent_backend.agent.trace.AgentRequestLogService;
import invoice_agent_backend.agent.trace.AgentToolTrace;
import invoice_agent_backend.agent.trace.AgentToolTraceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/*
 ** Agent Runtime v1.0 的 Supervisor。
 **
 ** 模型负责理解、选 Tool、组织结构化答案；
 ** Java Runtime 负责 requestId、Trace、待确认动作和安全边界。
 */
@Service
@ConditionalOnProperty(
        name = "agent.supervisor.enabled",
        havingValue = "true"
)
public class SpringAiSupervisorAgentService
        implements SupervisorAgentService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    SpringAiSupervisorAgentService.class
            );

    private static final Set<String> RISK_LEVELS =
            Set.of("UNKNOWN", "LOW", "MEDIUM", "HIGH");

    private static final String SYSTEM_PROMPT = """
            你是智能发票报销审核系统的 Supervisor Agent。

            你的职责是理解用户请求、选择合适的业务 Tool，
            并根据 Tool 返回的真实数据给出结构化结果。

            必须遵守以下规则：
            1. 不得编造任务、发票、规则命中、风险或审核结论。
            2. 只要问题涉及数据库中的具体任务事实，就必须调用 Tool。
            3. getTaskDetailTool、getInvoiceInfoTool、getRuleHitsTool、getAuditReportTool、listTasksByStatusTool 都是只读 Tool。
            4. prepareHumanReviewTool 只有在用户明确要求对具体任务 APPROVE 或 REJECT 时才允许调用。
            5. prepareHumanReviewTool 只准备待确认动作，不代表人工审核已经执行完成。
            6. 不得声称任务已经 APPROVE / REJECT，除非业务 Tool 返回的数据明确显示最终状态已经改变。
            7. 不得绕过 Workflow、State Machine 或 Transaction，也不得要求直接执行 SQL。
            8. 用户没有提供足够的任务 ID 时，不要猜测任务。
            9. riskLevel 只能输出 UNKNOWN、LOW、MEDIUM、HIGH。
            10. evidence 只能引用 Tool 返回事实；如果没有事实依据，返回空列表。
            11. nextAction 要说明真实可执行的下一步，不要虚构已经执行的操作。
            """;

    private final ChatClient chatClient;
    private final AgentToolTraceContext traceContext;
    private final AgentRequestLogService requestLogService;
    private final AgentActionContext actionContext;
    private final AgentActionService actionService;
    private final int maxMessageLength;

    public SpringAiSupervisorAgentService(
            ChatClient.Builder chatClientBuilder,
            InvoiceAuditAgentTools invoiceAuditAgentTools,
            AgentToolTraceContext traceContext,
            AgentRequestLogService requestLogService,
            AgentActionContext actionContext,
            AgentActionService actionService,
            @Value("${agent.supervisor.max-message-length:2000}")
            int maxMessageLength) {

        this.chatClient =
                chatClientBuilder
                        .defaultSystem(SYSTEM_PROMPT)
                        .defaultTools(
                                invoiceAuditAgentTools
                        )
                        .build();

        this.traceContext = traceContext;
        this.requestLogService = requestLogService;
        this.actionContext = actionContext;
        this.actionService = actionService;
        this.maxMessageLength =
                Math.max(200, maxMessageLength);
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

        if (safeMessage.length() > maxMessageLength) {
            throw new RuntimeException(
                    "Supervisor message 过长，最大允许 "
                            + maxMessageLength
                            + " 个字符"
            );
        }

        String requestId = newRequestId();

        traceContext.start(requestId);
        actionContext.start(requestId);
        requestLogService.start(
                requestId,
                safeMessage
        );

        try {
            SupervisorStructuredAnswer structuredAnswer =
                    chatClient
                            .prompt()
                            .user(safeMessage)
                            .call()
                            .entity(
                                    SupervisorStructuredAnswer.class
                            );

            sanitizeStructuredAnswer(
                    structuredAnswer
            );

            String answer =
                    structuredAnswer.toDisplayText();

            List<AgentToolTrace> toolTraces =
                    traceContext.snapshot();

            List<AgentPendingActionView> pendingActions =
                    actionContext.snapshot();

            requestLogService.complete(
                    requestId,
                    answer
            );

            return new SupervisorAgentResponse(
                    requestId,
                    answer,
                    structuredAnswer,
                    pendingActions,
                    toolTraces
            );

        } catch (RuntimeException e) {

            try {
                actionService.cancelPendingByRequestId(
                        requestId
                );
            } catch (RuntimeException cancelError) {
                log.warn(
                        "Failed to cancel pending Agent actions after request failure, requestId={}",
                        requestId,
                        cancelError
                );
            }

            requestLogService.fail(
                    requestId,
                    e.getMessage()
            );

            throw e;

        } finally {
            actionContext.clear();
            traceContext.clear();
        }
    }

    private void sanitizeStructuredAnswer(
            SupervisorStructuredAnswer answer) {

        if (answer == null) {
            throw new RuntimeException(
                    "Supervisor 返回了空的结构化结果"
            );
        }

        String summary = trim(answer.getSummary());

        if (summary == null) {
            throw new RuntimeException(
                    "Supervisor 结构化结果缺少 summary"
            );
        }

        answer.setSummary(limit(summary, 4000));
        answer.setIntent(
                limit(defaultText(answer.getIntent(), "UNKNOWN"), 200)
        );

        String riskLevel =
                defaultText(
                        answer.getRiskLevel(),
                        "UNKNOWN"
                ).toUpperCase();

        answer.setRiskLevel(
                RISK_LEVELS.contains(riskLevel)
                        ? riskLevel
                        : "UNKNOWN"
        );

        List<String> evidence =
                answer.getEvidence() == null
                        ? List.of()
                        : answer.getEvidence();

        List<String> safeEvidence =
                new ArrayList<>();

        for (String item : evidence) {
            String safeItem = trim(item);

            if (safeItem != null) {
                safeEvidence.add(
                        limit(safeItem, 500)
                );
            }

            if (safeEvidence.size() >= 10) {
                break;
            }
        }

        answer.setEvidence(safeEvidence);
        answer.setNextAction(
                limit(
                        defaultText(
                                answer.getNextAction(),
                                "无需额外操作"
                        ),
                        1000
                )
        );
    }

    private String defaultText(
            String value,
            String defaultValue) {

        String trimmed = trim(value);
        return trimmed == null
                ? defaultValue
                : trimmed;
    }

    private String trim(String value) {

        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty()
                ? null
                : trimmed;
    }

    private String limit(
            String value,
            int maxLength) {

        if (value == null
                || value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength);
    }

    private String newRequestId() {
        return "AGT-" + UUID.randomUUID();
    }
}
