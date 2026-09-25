package invoice_agent_backend.agent.trace;

import invoice_agent_backend.agent.model.AgentRequestTraceResult;
import invoice_agent_backend.agent.trace.impl.AgentRequestLogServiceImpl;
import invoice_agent_backend.mapper.AgentRequestLogMapper;
import invoice_agent_backend.mapper.AgentToolTraceMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentRequestLogServiceTest {

    @Test
    void shouldPersistRequestLifecycleAndAggregateToolTraces() {

        AgentRequestLogMapper requestLogMapper =
                mock(AgentRequestLogMapper.class);

        AgentToolTraceMapper toolTraceMapper =
                mock(AgentToolTraceMapper.class);

        AgentRequestLogServiceImpl service =
                new AgentRequestLogServiceImpl(
                        requestLogMapper,
                        toolTraceMapper
                );

        String requestId = "AGT-test-1";

        service.start(
                requestId,
                "任务 6 为什么需要人工复核？"
        );

        service.complete(
                requestId,
                "任务 6 命中了金额超限规则。"
        );

        verify(requestLogMapper)
                .insertAgentRequest(any(AgentRequestRecord.class));

        verify(requestLogMapper)
                .updateCompleted(
                        eq(requestId),
                        eq("任务 6 命中了金额超限规则。"),
                        any(LocalDateTime.class)
                );

        AgentRequestRecord request =
                new AgentRequestRecord();
        request.setRequestId(requestId);
        request.setStatus("COMPLETED");

        AgentToolTrace trace =
                new AgentToolTrace(
                        requestId,
                        "getRuleHitsTool",
                        "taskId=6",
                        "ruleHitCount=1",
                        true,
                        LocalDateTime.now()
                );

        when(requestLogMapper.selectByRequestId(requestId))
                .thenReturn(request);

        when(toolTraceMapper.selectByRequestId(requestId))
                .thenReturn(List.of(trace));

        AgentRequestTraceResult result =
                service.getRequestTrace(requestId);

        assertNotNull(result.getRequest());
        assertEquals(
                requestId,
                result.getRequest().getRequestId()
        );
        assertEquals(
                1,
                result.getToolTraces().size()
        );
        assertEquals(
                "getRuleHitsTool",
                result.getToolTraces().get(0).getToolName()
        );
    }
}
