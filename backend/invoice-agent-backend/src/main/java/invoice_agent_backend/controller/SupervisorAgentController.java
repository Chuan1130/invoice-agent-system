package invoice_agent_backend.controller;

import invoice_agent_backend.agent.model.SupervisorAgentRequest;
import invoice_agent_backend.agent.model.SupervisorAgentResponse;
import invoice_agent_backend.agent.supervisor.SupervisorAgentService;
import invoice_agent_backend.common.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 ** Supervisor Agent 的 HTTP 入口。
 **
 ** 例如：
 ** POST /api/agent/supervisor
 ** { "message": "任务 6 为什么需要人工复核？" }
 */
@RestController
@RequestMapping("/api/agent")
public class SupervisorAgentController {

    private final SupervisorAgentService
            supervisorAgentService;

    public SupervisorAgentController(
            SupervisorAgentService supervisorAgentService) {

        this.supervisorAgentService =
                supervisorAgentService;
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
}
