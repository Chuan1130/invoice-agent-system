package invoice_agent_backend.agent.model;

/*
 ** Supervisor Agent 的自然语言请求。
 **
 ** 第一版只接收 message，
 ** taskId 等参数由模型从用户问题中识别后传给 Tool。
 */
public class SupervisorAgentRequest {

    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
