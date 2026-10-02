package invoice_agent_backend.agent.tool;

import invoice_agent_backend.agent.trace.AgentToolTraceContext;
import invoice_agent_backend.mapper.AgentToolTraceMapper;
import invoice_agent_backend.service.AuditTaskService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

class InvoiceAuditAgentToolCatalogTest {

    @Test
    void shouldExposeExpectedReadOnlyToolContracts() {

        AuditTaskService auditTaskService =
                mock(AuditTaskService.class);

        AgentToolTraceMapper traceMapper =
                mock(AgentToolTraceMapper.class);

        InvoiceAuditAgentTools tools =
                new InvoiceAuditAgentTools(
                        auditTaskService,
                        new AgentToolTraceContext(traceMapper)
                );

        ToolCallback[] callbacks =
                ToolCallbacks.from(tools);

        Set<String> toolNames =
                Arrays.stream(callbacks)
                        .map(callback ->
                                callback
                                        .getToolDefinition()
                                        .name())
                        .collect(Collectors.toSet());

        assertEquals(
                Set.of(
                        "getTaskDetailTool",
                        "getInvoiceInfoTool",
                        "getRuleHitsTool",
                        "getAuditReportTool",
                        "listTasksByStatusTool"
                ),
                toolNames
        );

        for (ToolCallback callback : callbacks) {
            assertFalse(
                    callback
                            .getToolDefinition()
                            .description()
                            .isBlank()
            );

            assertFalse(
                    callback
                            .getToolDefinition()
                            .inputSchema()
                            .isBlank()
            );
        }
    }
}
