package invoice_agent_backend.controller;

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
 ** 用自然语言发起一次 Agent 请求。
 **
 ** GET /api/agent/requests/{requestId}
 ** 按 requestId 回看这次请求和它实际调用过的 Tool。
 */
@RestController
@RequestMapping("/api/agent")
public class SupervisorAgentController {

    private final SupervisorAgentService
            supervisorAgentService;

    private final AgentRequestLogService
            requestLogService;

    public SupervisorAgentController(
            SupervisorAgentService supervisorAgentService,
            AgentRequestLogService requestLogService) {

        this.supervisorAgentService =
                supervisorAgentService;
        this.requestLogService =
                requestLogService;
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
}
