package invoice_agent_backend.agent.action;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AgentActionContextTest {

    @Test
    void shouldKeepPreparedActionInsideCurrentRequest() {

        AgentActionContext context =
                new AgentActionContext();

        context.start("AGT-1");

        AgentActionRequestRecord record =
                new AgentActionRequestRecord();
        record.setRequestId("AGT-1");
        record.setActionToken("ACT-1");
        record.setActionType("HUMAN_REVIEW");
        record.setTaskId(6L);
        record.setDecision("APPROVE");
        record.setStatus("PENDING_CONFIRMATION");
        record.setExpiresAt(
                LocalDateTime.now().plusMinutes(10)
        );

        context.register(record);
        context.register(record);

        assertEquals(1, context.snapshot().size());
        assertEquals(
                "ACT-1",
                context.snapshot()
                        .get(0)
                        .getConfirmationToken()
        );

        context.clear();

        assertThrows(
                IllegalStateException.class,
                context::currentRequestId
        );
    }
}
