package invoice_agent_backend.agent.trace;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/*
 ** 保存一次同步 Supervisor 请求里的 Tool 调用轨迹。
 **
 ** 当前 ChatClient 使用阻塞调用，所以先用 ThreadLocal 将一次请求
 ** 的轨迹隔离开。Supervisor 结束时通过 requestId 独立落库。
 ** 异步或流式调用仍需要显式传递上下文，不能直接复用 ThreadLocal。
 */
@Component
public class AgentToolTraceContext {

    private static final int MAX_SUMMARY_LENGTH = 500;

    private final ThreadLocal<List<AgentToolTrace>>
            traces =
            ThreadLocal.withInitial(ArrayList::new);

    public void start() {
        traces.set(new ArrayList<>());
    }

    public void recordSuccess(
            String toolName,
            String inputSummary,
            String resultSummary) {

        traces.get().add(
                new AgentToolTrace(
                        toolName,
                        limit(inputSummary),
                        limit(resultSummary),
                        true,
                        LocalDateTime.now()
                )
        );
    }

    public void recordFailure(
            String toolName,
            String inputSummary,
            String errorMessage) {

        traces.get().add(
                new AgentToolTrace(
                        toolName,
                        limit(inputSummary),
                        limit(errorMessage),
                        false,
                        LocalDateTime.now()
                )
        );
    }

    public List<AgentToolTrace> snapshot() {
        return new ArrayList<>(traces.get());
    }

    public void clear() {
        traces.remove();
    }

    private String limit(String value) {

        if (value == null) {
            return null;
        }

        if (value.length() <= MAX_SUMMARY_LENGTH) {
            return value;
        }

        return value.substring(
                0,
                MAX_SUMMARY_LENGTH
        ) + "...";
    }
}

