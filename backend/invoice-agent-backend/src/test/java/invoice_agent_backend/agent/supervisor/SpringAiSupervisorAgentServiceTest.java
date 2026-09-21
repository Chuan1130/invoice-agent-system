package invoice_agent_backend.agent.supervisor;

import invoice_agent_backend.agent.model.SupervisorAgentResponse;
import invoice_agent_backend.agent.supervisor.impl.SpringAiSupervisorAgentService;
import invoice_agent_backend.agent.tool.InvoiceAuditAgentTools;
import invoice_agent_backend.agent.trace.AgentToolTrace;
import invoice_agent_backend.agent.trace.AgentToolTraceContext;
import invoice_agent_backend.agent.trace.AgentToolTraceStore;
import invoice_agent_backend.service.AuditTaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SpringAiSupervisorAgentServiceTest {
    private final ChatModel model = mock(ChatModel.class);
    private final AuditTaskService business = mock(AuditTaskService.class);
    private final AgentToolTraceStore store = mock(AgentToolTraceStore.class);
    private final AgentToolTraceContext context = new AgentToolTraceContext();
    private final ToolCallingManager manager = ToolCallingManager.builder().build();
    private SpringAiSupervisorAgentService supervisor;

    @BeforeEach
    void setUp() {
        when(model.getDefaultOptions()).thenReturn(ToolCallingChatOptions.builder().build());
        supervisor = new SpringAiSupervisorAgentService(ChatClient.builder(model),
                new InvoiceAuditAgentTools(business, context), context, store);
    }

    // The model is scripted; JSON arguments are dispatched by the real Spring AI manager
    // through the callbacks actually registered by the Supervisor's ChatClient.
    private void callTool(Prompt prompt, String name, String arguments) {
        var options = (ToolCallingChatOptions) prompt.getOptions();
        assertEquals(Set.of("getTaskDetailTool", "getInvoiceInfoTool", "getRuleHitsTool",
                        "getAuditReportTool", "listTasksByStatusTool"),
                options.getToolCallbacks().stream()
                        .map(c -> c.getToolDefinition().name()).collect(Collectors.toSet()));
        var message = AssistantMessage.builder().content("").toolCalls(List.of(
                new AssistantMessage.ToolCall("call-1", "function", name, arguments))).build();
        var result = manager.executeToolCalls(prompt, new ChatResponse(List.of(new Generation(message))));
        assertTrue(result.conversationHistory().stream().anyMatch(ToolResponseMessage.class::isInstance));
    }

    private ChatResponse answer() {
        return new ChatResponse(List.of(new Generation(new AssistantMessage("No risk rules found."))));
    }

    @Test
    void dispatchesJsonToolCallsAndPersistsOrderedTraces() {
        when(business.getRuleHitsByTaskId(6L)).thenReturn(List.of());
        when(model.call(any(Prompt.class))).thenAnswer(invocation -> {
            Prompt prompt = invocation.getArgument(0);
            callTool(prompt, "getRuleHitsTool", "{\"taskId\":6}");
            callTool(prompt, "getRuleHitsTool", "{\"taskId\":6}");
            return answer();
        });
        SupervisorAgentResponse response = supervisor.ask("  Explain task 6  ");
        assertNotNull(UUID.fromString(response.getRequestId()));
        assertEquals("No risk rules found.", response.getAnswer());
        assertEquals(2, response.getToolTraces().size());
        assertTrue(response.getToolTraces().stream().allMatch(AgentToolTrace::getSuccess));
        verify(store).save(response.getRequestId(), response.getToolTraces());
        verify(business, times(2)).getRuleHitsByTaskId(6L);
        verifyNoMoreInteractions(business);
        assertTrue(context.snapshot().isEmpty());
    }

    @Test
    void noToolResponsesHaveDistinctRequestIdsAndEmptyTraces() {
        when(model.call(any(Prompt.class))).thenReturn(answer());
        var first = supervisor.ask("hello");
        var second = supervisor.ask("hello again");
        assertNotEquals(first.getRequestId(), second.getRequestId());
        assertTrue(second.getToolTraces().isEmpty());
        verify(store).save(first.getRequestId(), List.of());
        verify(store).save(second.getRequestId(), List.of());
        verifyNoInteractions(business);
    }

    @Test
    void savesCompletedCallsWhenModelFailsAndNextRequestStartsClean() {
        when(business.getRuleHitsByTaskId(6L)).thenReturn(List.of());
        RuntimeException failure = new IllegalStateException("model unavailable");
        when(model.call(any(Prompt.class))).thenAnswer(invocation -> {
            callTool(invocation.getArgument(0), "getRuleHitsTool", "{\"taskId\":6}");
            throw failure;
        }).thenReturn(answer());
        assertSame(failure, assertThrows(RuntimeException.class, () -> supervisor.ask("task 6")));
        verify(store).save(anyString(), argThat(traces -> traces.size() == 1 && traces.get(0).getSuccess()));
        assertTrue(context.snapshot().isEmpty());
        assertTrue(supervisor.ask("hello").getToolTraces().isEmpty());
    }

    @Test
    void savesFailedToolCalls() {
        when(business.getRuleHitsByTaskId(6L)).thenThrow(new IllegalStateException("task unavailable"));
        when(model.call(any(Prompt.class))).thenAnswer(invocation -> {
            callTool(invocation.getArgument(0), "getRuleHitsTool", "{\"taskId\":6}");
            return answer();
        });
        supervisor.ask("task 6");
        verify(store).save(anyString(), argThat(traces -> traces.size() == 1 && !traces.get(0).getSuccess()));
        assertTrue(context.snapshot().isEmpty());
    }

    @Test
    void rejectsUnregisteredWriteToolWithoutInvokingBusiness() {
        when(model.call(any(Prompt.class))).thenAnswer(invocation -> {
            callTool(invocation.getArgument(0), "humanReviewTool", "{\"taskId\":6}");
            return answer();
        });
        assertThrows(RuntimeException.class, () -> supervisor.ask("approve task 6"));
        verifyNoInteractions(business);
        verify(store).save(anyString(), eq(List.of()));
    }

    @Test
    void persistenceFailureFailsRequestAndClearsContext() {
        when(model.call(any(Prompt.class))).thenReturn(answer());
        RuntimeException failure = new IllegalStateException("database unavailable");
        doThrow(failure).when(store).save(anyString(), anyList());
        assertSame(failure, assertThrows(RuntimeException.class, () -> supervisor.ask("hello")));
        assertTrue(context.snapshot().isEmpty());
    }

    @Test
    void persistenceFailureDoesNotReplaceOriginalModelFailure() {
        RuntimeException modelFailure = new IllegalStateException("model unavailable");
        RuntimeException dbFailure = new IllegalStateException("database unavailable");
        when(model.call(any(Prompt.class))).thenThrow(modelFailure);
        doThrow(dbFailure).when(store).save(anyString(), anyList());
        assertSame(modelFailure, assertThrows(RuntimeException.class, () -> supervisor.ask("hello")));
        assertArrayEquals(new Throwable[]{dbFailure}, modelFailure.getSuppressed());
        assertTrue(context.snapshot().isEmpty());
    }

    @Test
    void blankMessageDoesNotCallModelOrStore() {
        assertThrows(RuntimeException.class, () -> supervisor.ask("  "));
        assertThrows(RuntimeException.class, () -> supervisor.ask(null));
        verify(model, never()).call(any(Prompt.class));
        verifyNoInteractions(store, business);
    }
}
