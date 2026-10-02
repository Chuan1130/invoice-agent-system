package invoice_agent_backend.agent.action;

import invoice_agent_backend.agent.action.impl.AgentActionServiceImpl;
import invoice_agent_backend.agent.model.AgentActionConfirmRequest;
import invoice_agent_backend.agent.model.AgentActionExecutionResult;
import invoice_agent_backend.constant.AuditTaskStatus;
import invoice_agent_backend.entity.AuditTask;
import invoice_agent_backend.mapper.AgentActionRequestMapper;
import invoice_agent_backend.service.AuditTaskService;
import invoice_agent_backend.vo.AuditTaskDetailResult;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentActionServiceTest {

    @Test
    void shouldPrepareHumanReviewWithoutChangingBusinessState() {

        AgentActionRequestMapper mapper =
                mock(AgentActionRequestMapper.class);
        AuditTaskService auditTaskService =
                mock(AuditTaskService.class);

        AgentActionServiceImpl service =
                new AgentActionServiceImpl(
                        mapper,
                        auditTaskService,
                        true,
                        10
                );

        AuditTask task = waitingHumanReviewTask();

        when(auditTaskService.getTaskById(6L))
                .thenReturn(task);
        when(mapper.selectByDedupeKey(anyString()))
                .thenReturn(null);
        when(mapper.insertAction(any()))
                .thenReturn(1);

        AgentActionRequestRecord result =
                service.prepareHumanReview(
                        "AGT-test",
                        6L,
                        "approve",
                        "checked"
                );

        assertEquals(
                "PENDING_CONFIRMATION",
                result.getStatus()
        );
        assertEquals("APPROVE", result.getDecision());
        assertEquals(6L, result.getTaskId());
        assertTrue(
                result.getActionToken().startsWith("ACT-")
        );

        verify(auditTaskService, never())
                .humanReview(any(), any());
    }

    @Test
    void shouldRejectPrepareWhenTaskIsNotWaitingForHumanReview() {

        AgentActionRequestMapper mapper =
                mock(AgentActionRequestMapper.class);
        AuditTaskService auditTaskService =
                mock(AuditTaskService.class);

        AgentActionServiceImpl service =
                new AgentActionServiceImpl(
                        mapper,
                        auditTaskService,
                        true,
                        10
                );

        AuditTask task = new AuditTask();
        task.setId(6L);
        task.setStatus(AuditTaskStatus.COMPLETED);
        task.setNeedHumanReview(false);

        when(auditTaskService.getTaskById(6L))
                .thenReturn(task);

        assertThrows(
                RuntimeException.class,
                () -> service.prepareHumanReview(
                        "AGT-test",
                        6L,
                        "APPROVE",
                        null
                )
        );

        verify(mapper, never())
                .insertAction(any());
    }

    @Test
    void shouldExecuteHumanReviewOnlyAfterConfirmation() {

        AgentActionRequestMapper mapper =
                mock(AgentActionRequestMapper.class);
        AuditTaskService auditTaskService =
                mock(AuditTaskService.class);

        AgentActionServiceImpl service =
                new AgentActionServiceImpl(
                        mapper,
                        auditTaskService,
                        true,
                        10
                );

        AgentActionRequestRecord action =
                pendingAction();

        when(mapper.selectByTokenForUpdate("ACT-test"))
                .thenReturn(action);

        AuditTask completedTask = new AuditTask();
        completedTask.setId(6L);
        completedTask.setStatus(AuditTaskStatus.COMPLETED);
        completedTask.setFinalDecision(
                "APPROVED_BY_HUMAN"
        );

        AuditTaskDetailResult detail =
                new AuditTaskDetailResult(
                        completedTask,
                        null,
                        List.of(),
                        null,
                        List.of()
                );

        when(auditTaskService.humanReview(
                eq(6L),
                any()
        )).thenReturn(detail);

        when(mapper.updateExecuted(
                eq(1L),
                eq("tester"),
                anyString(),
                any(LocalDateTime.class)
        )).thenReturn(1);

        AgentActionConfirmRequest confirmRequest =
                new AgentActionConfirmRequest();
        confirmRequest.setConfirmedBy("tester");
        confirmRequest.setComment("confirmed");

        AgentActionExecutionResult result =
                service.confirmHumanReview(
                        "ACT-test",
                        confirmRequest
                );

        assertTrue(result.getExecutedNow());
        assertEquals(
                "EXECUTED",
                result.getAction().getStatus()
        );

        verify(auditTaskService)
                .humanReview(eq(6L), any());
    }

    @Test
    void repeatedConfirmationShouldBeIdempotent() {

        AgentActionRequestMapper mapper =
                mock(AgentActionRequestMapper.class);
        AuditTaskService auditTaskService =
                mock(AuditTaskService.class);

        AgentActionServiceImpl service =
                new AgentActionServiceImpl(
                        mapper,
                        auditTaskService,
                        true,
                        10
                );

        AgentActionRequestRecord action =
                pendingAction();
        action.setStatus("EXECUTED");
        action.setExecutedAt(LocalDateTime.now());

        when(mapper.selectByTokenForUpdate("ACT-test"))
                .thenReturn(action);

        AuditTask completedTask = new AuditTask();
        completedTask.setId(6L);
        completedTask.setStatus(AuditTaskStatus.COMPLETED);

        when(auditTaskService.getTaskDetail(6L))
                .thenReturn(
                        new AuditTaskDetailResult(
                                completedTask,
                                null,
                                List.of(),
                                null,
                                List.of()
                        )
                );

        AgentActionConfirmRequest confirmRequest =
                new AgentActionConfirmRequest();
        confirmRequest.setConfirmedBy("tester");

        AgentActionExecutionResult result =
                service.confirmHumanReview(
                        "ACT-test",
                        confirmRequest
                );

        assertFalse(result.getExecutedNow());

        verify(auditTaskService, never())
                .humanReview(any(), any());
        verify(mapper, never())
                .updateExecuted(
                        any(),
                        anyString(),
                        anyString(),
                        any()
                );
    }

    @Test
    void expiredConfirmationShouldNotExecuteBusinessWrite() {

        AgentActionRequestMapper mapper =
                mock(AgentActionRequestMapper.class);
        AuditTaskService auditTaskService =
                mock(AuditTaskService.class);

        AgentActionServiceImpl service =
                new AgentActionServiceImpl(
                        mapper,
                        auditTaskService,
                        true,
                        10
                );

        AgentActionRequestRecord action =
                pendingAction();
        action.setExpiresAt(
                LocalDateTime.now().minusMinutes(1)
        );

        when(mapper.selectByTokenForUpdate("ACT-test"))
                .thenReturn(action);
        when(mapper.updateStatusIfPending(
                1L,
                "EXPIRED"
        )).thenReturn(1);

        AgentActionConfirmRequest confirmRequest =
                new AgentActionConfirmRequest();
        confirmRequest.setConfirmedBy("tester");

        AgentActionExecutionResult result =
                service.confirmHumanReview(
                        "ACT-test",
                        confirmRequest
                );

        assertFalse(result.getExecutedNow());
        assertEquals(
                "EXPIRED",
                result.getAction().getStatus()
        );

        verify(auditTaskService, never())
                .humanReview(any(), any());
    }

    @Test
    void disabledActionFeatureShouldFailClosed() {

        AgentActionServiceImpl service =
                new AgentActionServiceImpl(
                        mock(AgentActionRequestMapper.class),
                        mock(AuditTaskService.class),
                        false,
                        10
                );

        assertThrows(
                RuntimeException.class,
                () -> service.prepareHumanReview(
                        "AGT-test",
                        6L,
                        "APPROVE",
                        null
                )
        );
    }

    private AuditTask waitingHumanReviewTask() {

        AuditTask task = new AuditTask();
        task.setId(6L);
        task.setStatus(AuditTaskStatus.AUDIT_DONE);
        task.setNeedHumanReview(true);
        return task;
    }

    private AgentActionRequestRecord pendingAction() {

        AgentActionRequestRecord action =
                new AgentActionRequestRecord();
        action.setId(1L);
        action.setActionToken("ACT-test");
        action.setDedupeKey(
                "AGT-test:HUMAN_REVIEW:6:APPROVE"
        );
        action.setRequestId("AGT-test");
        action.setActionType("HUMAN_REVIEW");
        action.setTaskId(6L);
        action.setDecision("APPROVE");
        action.setStatus("PENDING_CONFIRMATION");
        action.setCreatedAt(LocalDateTime.now());
        action.setExpiresAt(
                LocalDateTime.now().plusMinutes(10)
        );
        return action;
    }
}
