package invoice_agent_backend.agent.trace;

import invoice_agent_backend.mapper.AgentToolTraceMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/*
 ** 保存一次同步 Supervisor 请求里的 Tool 调用轨迹。
 **
 ** requestId 负责把同一次请求串起来；ThreadLocal 继续负责隔离同步请求。
 ** 每次 Tool 调用会同时保留在当前请求内存中，并尽量持久化到数据库。
 */
@Component
public class AgentToolTraceContext {

    private static final Logger log =
            LoggerFactory.getLogger(AgentToolTraceContext.class);

    private static final int MAX_SUMMARY_LENGTH = 500;

    private final AgentToolTraceMapper traceMapper;

    private final ThreadLocal<String> requestIds =
            new ThreadLocal<>();

    private final ThreadLocal<List<AgentToolTrace>> traces =
            ThreadLocal.withInitial(ArrayList::new);

    public AgentToolTraceContext(
            AgentToolTraceMapper traceMapper) {

        this.traceMapper = traceMapper;
    }

    public void start(String requestId) {

        if (requestId == null
                || requestId.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Agent requestId 不能为空"
            );
        }

        requestIds.set(requestId.trim());
        traces.set(new ArrayList<>());
    }

    public void recordSuccess(
            String toolName,
            String inputSummary,
            String resultSummary) {

        appendTrace(
                toolName,
                inputSummary,
                resultSummary,
                true
        );
    }

    public void recordFailure(
            String toolName,
            String inputSummary,
            String errorMessage) {

        appendTrace(
                toolName,
                inputSummary,
                errorMessage,
                false
        );
    }

    public List<AgentToolTrace> snapshot() {
        return new ArrayList<>(traces.get());
    }

    public void clear() {
        traces.remove();
        requestIds.remove();
    }

    private void appendTrace(
            String toolName,
            String inputSummary,
            String resultSummary,
            boolean success) {

        AgentToolTrace trace =
                new AgentToolTrace(
                        requestIds.get(),
                        toolName,
                        limit(inputSummary),
                        limit(resultSummary),
                        success,
                        LocalDateTime.now()
                );

        traces.get().add(trace);

        try {
            traceMapper.insertAgentToolTrace(trace);
        } catch (RuntimeException e) {
            /*
             ** Trace 落库失败不能反过来破坏真实业务查询。
             ** 内存轨迹仍会随本次响应返回，数据库问题单独记录日志排查。
             */
            log.warn(
                    "Agent Tool Trace persistence failed, requestId={}, toolName={}",
                    trace.getRequestId(),
                    trace.getToolName(),
                    e
            );
        }
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
