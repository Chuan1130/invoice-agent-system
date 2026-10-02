package invoice_agent_backend.agent.action;

import invoice_agent_backend.agent.model.AgentPendingActionView;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/*
 ** 保存一次同步 Supervisor 请求中准备出来的待确认动作。
 **
 ** Tool 不把 confirmationToken 返回给模型；
 ** token 通过这个上下文直接附加到后端响应，避免模型改写或编造凭证。
 */
@Component
public class AgentActionContext {

    private final ThreadLocal<String> requestIds =
            new ThreadLocal<>();

    private final ThreadLocal<List<AgentPendingActionView>> actions =
            ThreadLocal.withInitial(ArrayList::new);

    public void start(String requestId) {

        if (requestId == null
                || requestId.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Agent requestId 不能为空"
            );
        }

        requestIds.set(requestId.trim());
        actions.set(new ArrayList<>());
    }

    public String currentRequestId() {

        String requestId = requestIds.get();

        if (requestId == null
                || requestId.isBlank()) {
            throw new IllegalStateException(
                    "当前没有活动的 Agent 请求上下文"
            );
        }

        return requestId;
    }

    public void register(
            AgentActionRequestRecord record) {

        if (record == null) {
            throw new IllegalArgumentException(
                    "待确认动作不能为空"
            );
        }

        if (!currentRequestId().equals(
                record.getRequestId())) {
            throw new IllegalStateException(
                    "待确认动作不属于当前 Agent 请求"
            );
        }

        AgentPendingActionView view =
                AgentPendingActionView.from(record);

        boolean alreadyRegistered =
                actions.get()
                        .stream()
                        .anyMatch(existing ->
                                existing
                                        .getConfirmationToken()
                                        .equals(
                                                view.getConfirmationToken()
                                        )
                        );

        if (!alreadyRegistered) {
            actions.get().add(view);
        }
    }

    public List<AgentPendingActionView> snapshot() {
        return List.copyOf(actions.get());
    }

    public void clear() {
        actions.remove();
        requestIds.remove();
    }
}
