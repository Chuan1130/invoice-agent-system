package invoice_agent_backend.agent.trace.impl;

import invoice_agent_backend.agent.model.AgentRequestTraceResult;
import invoice_agent_backend.agent.trace.AgentRequestLogService;
import invoice_agent_backend.agent.trace.AgentRequestRecord;
import invoice_agent_backend.agent.trace.AgentToolTrace;
import invoice_agent_backend.mapper.AgentRequestLogMapper;
import invoice_agent_backend.mapper.AgentToolTraceMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AgentRequestLogServiceImpl
        implements AgentRequestLogService {

    private static final Logger log =
            LoggerFactory.getLogger(AgentRequestLogServiceImpl.class);

    private static final String RUNNING = "RUNNING";
    private static final String COMPLETED = "COMPLETED";
    private static final String FAILED = "FAILED";

    private final AgentRequestLogMapper requestLogMapper;
    private final AgentToolTraceMapper toolTraceMapper;

    public AgentRequestLogServiceImpl(
            AgentRequestLogMapper requestLogMapper,
            AgentToolTraceMapper toolTraceMapper) {

        this.requestLogMapper = requestLogMapper;
        this.toolTraceMapper = toolTraceMapper;
    }

    @Override
    public void start(
            String requestId,
            String userMessage) {

        AgentRequestRecord record =
                new AgentRequestRecord();

        record.setRequestId(requestId);
        record.setUserMessage(userMessage);
        record.setStatus(RUNNING);
        record.setStartedAt(LocalDateTime.now());

        try {
            requestLogMapper.insertAgentRequest(record);
        } catch (RuntimeException e) {
            /*
             ** Agent 审计日志属于可观测性能力。
             ** 日志表临时异常时，不应该阻断真实的只读业务查询。
             */
            log.warn(
                    "Agent request start persistence failed, requestId={}",
                    requestId,
                    e
            );
        }
    }

    @Override
    public void complete(
            String requestId,
            String answer) {

        try {
            requestLogMapper.updateCompleted(
                    requestId,
                    answer,
                    LocalDateTime.now()
            );
        } catch (RuntimeException e) {
            log.warn(
                    "Agent request completion persistence failed, requestId={}",
                    requestId,
                    e
            );
        }
    }

    @Override
    public void fail(
            String requestId,
            String errorMessage) {

        try {
            requestLogMapper.updateFailed(
                    requestId,
                    errorMessage,
                    LocalDateTime.now()
            );
        } catch (RuntimeException e) {
            log.warn(
                    "Agent request failure persistence failed, requestId={}",
                    requestId,
                    e
            );
        }
    }

    @Override
    public AgentRequestTraceResult getRequestTrace(
            String requestId) {

        if (requestId == null
                || requestId.trim().isEmpty()) {
            throw new RuntimeException(
                    "Agent requestId 不能为空"
            );
        }

        String safeRequestId = requestId.trim();

        AgentRequestRecord request =
                requestLogMapper
                        .selectByRequestId(safeRequestId);

        if (request == null) {
            throw new RuntimeException(
                    "Agent 请求记录不存在"
            );
        }

        List<AgentToolTrace> toolTraces =
                toolTraceMapper
                        .selectByRequestId(safeRequestId);

        return new AgentRequestTraceResult(
                request,
                toolTraces == null
                        ? List.of()
                        : toolTraces
        );
    }
}
