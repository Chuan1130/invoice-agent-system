package invoice_agent_backend.controller;

import invoice_agent_backend.agent.action.AgentActionService;
import invoice_agent_backend.agent.model.AgentActionConfirmRequest;
import invoice_agent_backend.agent.model.AgentActionExecutionResult;
import invoice_agent_backend.agent.model.AgentPendingActionView;
import invoice_agent_backend.agent.model.AgentRequestTraceResult;
import invoice_agent_backend.agent.model.SupervisorAgentRequest;
import invoice_agent_backend.agent.model.SupervisorAgentResponse;
import invoice_agent_backend.agent.supervisor.SupervisorAgentService;
import invoice_agent_backend.agent.trace.AgentRequestLogService;
import invoice_agent_backend.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 ** Supervisor Agent 的 HTTP 入口。
 **
 ** POST /api/agent/supervisor
 ** 发起自然语言 Agent 请求。
 **
 ** GET /api/agent/requests/{requestId}
 ** 回放 Agent 请求和 Tool Trace。
 **
 ** POST /api/agent/actions/{confirmationToken}/confirm
 ** 用户显式确认待执行写操作。
 */
@RestController
@RequestMapping("/api/agent")
public class SupervisorAgentController {

    private final SupervisorAgentService
            supervisorAgentService;

    private final AgentRequestLogService
            requestLogService;

    private final AgentActionService
            actionService;

    public SupervisorAgentController(
            SupervisorAgentService supervisorAgentService,
            AgentRequestLogService requestLogService,
            AgentActionService actionService) {

        this.supervisorAgentService =
                supervisorAgentService;
        this.requestLogService =
                requestLogService;
        this.actionService =
                actionService;
    }

    @PostMapping("/supervisor")
    public ApiResponse<SupervisorAgentResponse>
    askSupervisor(
            @RequestBody
            SupervisorAgentRequest request) {

        if (request == null) {
            throw new RuntimeException(
                    "Supervisor 请求不能为空"
            );
        }

        return ApiResponse.success(
                supervisorAgentService.ask(
                        request.getMessage()
                )
        );
    }

    @GetMapping("/requests/{requestId}")
    public ApiResponse<AgentRequestTraceResult>
    getRequestTrace(
            @PathVariable String requestId) {

        return ApiResponse.success(
                requestLogService
                        .getRequestTrace(requestId)
        );
    }

    @GetMapping("/actions/{confirmationToken}")
    public ApiResponse<AgentPendingActionView>
    getAction(
            @PathVariable String confirmationToken) {

        return ApiResponse.success(
                actionService.getAction(
                        confirmationToken
                )
        );
    }

    @PostMapping("/actions/{confirmationToken}/confirm")
    public ApiResponse<AgentActionExecutionResult>
    confirmAction(
            @PathVariable String confirmationToken,
            @RequestBody AgentActionConfirmRequest request) {

        return ApiResponse.success(
                actionService.confirmHumanReview(
                        confirmationToken,
                        request
                )
        );
    }

    @PostMapping("/actions/{confirmationToken}/cancel")
    public ApiResponse<AgentPendingActionView>
    cancelAction(
            @PathVariable String confirmationToken) {

        return ApiResponse.success(
                actionService.cancelAction(
                        confirmationToken
                )
        );
    }
}
