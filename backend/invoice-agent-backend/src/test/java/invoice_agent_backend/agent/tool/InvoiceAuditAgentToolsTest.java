package invoice_agent_backend.agent.tool;

import invoice_agent_backend.agent.trace.AgentToolTraceContext;
import invoice_agent_backend.entity.AuditTask;
import invoice_agent_backend.service.AuditTaskService;
import invoice_agent_backend.vo.AuditTaskDetailResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InvoiceAuditAgentToolsTest {

    @Test
    void getTaskDetailToolShouldReturnBusinessSummaryAndTrace() {

        AuditTaskService auditTaskService =
                mock(AuditTaskService.class);

        AgentToolTraceContext traceContext =
                new AgentToolTraceContext();

        InvoiceAuditAgentTools tools =
                new InvoiceAuditAgentTools(
                        auditTaskService,
                        traceContext
                );

        AuditTask task = new AuditTask();
        task.setId(6L);
        task.setTaskNo("TASK-6");
        task.setStatus("AUDIT_DONE");
        task.setFinalDecision("NEED_HUMAN_REVIEW");
        task.setNeedHumanReview(true);

        AuditTaskDetailResult detail =
                new AuditTaskDetailResult(
                        task,
                        null,
                        List.of(),
                        null,
                        List.of()
                );

        when(auditTaskService.getTaskDetail(6L))
                .thenReturn(detail);

        traceContext.start();

        String result =
                tools.getTaskDetailTool(6L);

        assertTrue(
                result.contains("status=AUDIT_DONE")
        );

        assertTrue(
                result.contains(
                        "finalDecision=NEED_HUMAN_REVIEW"
                )
        );

        assertEquals(
                1,
                traceContext.snapshot().size()
        );

        assertEquals(
                "getTaskDetailTool",
                traceContext
                        .snapshot()
                        .get(0)
                        .getToolName()
        );

        traceContext.clear();
    }
}
