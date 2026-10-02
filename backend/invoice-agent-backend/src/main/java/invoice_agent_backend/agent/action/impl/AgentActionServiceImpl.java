package invoice_agent_backend.agent.action.impl;

import invoice_agent_backend.agent.action.AgentActionRequestRecord;
import invoice_agent_backend.agent.action.AgentActionService;
import invoice_agent_backend.agent.model.AgentActionConfirmRequest;
import invoice_agent_backend.agent.model.AgentActionExecutionResult;
import invoice_agent_backend.agent.model.AgentPendingActionView;
import invoice_agent_backend.constant.AuditTaskStatus;
import invoice_agent_backend.dto.HumanReviewRequest;
import invoice_agent_backend.entity.AuditTask;
import invoice_agent_backend.mapper.AgentActionRequestMapper;
import invoice_agent_backend.service.AuditTaskService;
import invoice_agent_backend.vo.AuditTaskDetailResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/*
 ** Agent 可执行动作的安全边界。
 **
 ** LLM 只能 prepare，不能直接改变发票任务状态。
 ** 真正 humanReview 必须由用户拿 confirmationToken 再确认一次。
 */
@Service
public class AgentActionServiceImpl
        implements AgentActionService {

    private static final String ACTION_HUMAN_REVIEW =
            "HUMAN_REVIEW";

    private static final String PENDING_CONFIRMATION =
            "PENDING_CONFIRMATION";

    private static final String EXECUTED =
            "EXECUTED";

    private static final String CANCELLED =
            "CANCELLED";

    private static final String EXPIRED =
            "EXPIRED";

    private static final Set<String> HUMAN_DECISIONS =
            Set.of("APPROVE", "REJECT");

    private final AgentActionRequestMapper actionMapper;
    private final AuditTaskService auditTaskService;
    private final boolean humanReviewActionEnabled;
    private final long confirmationTtlMinutes;

    public AgentActionServiceImpl(
            AgentActionRequestMapper actionMapper,
            AuditTaskService auditTaskService,
            @Value("${agent.action.human-review.enabled:false}")
            boolean humanReviewActionEnabled,
            @Value("${agent.action.confirmation-ttl-minutes:10}")
            long confirmationTtlMinutes) {

        this.actionMapper = actionMapper;
        this.auditTaskService = auditTaskService;
        this.humanReviewActionEnabled =
                humanReviewActionEnabled;
        this.confirmationTtlMinutes =
                Math.max(1L, confirmationTtlMinutes);
    }

    @Override
    @Transactional
    public AgentActionRequestRecord prepareHumanReview(
            String requestId,
            Long taskId,
            String decision,
            String proposedComment) {

        ensureEnabled();

        String safeRequestId =
                requireText(requestId, "Agent requestId 不能为空");

        if (taskId == null) {
            throw new RuntimeException(
                    "人工复核任务 ID 不能为空"
            );
        }

        String safeDecision =
                requireText(decision, "人工复核 decision 不能为空")
                        .toUpperCase();

        if (!HUMAN_DECISIONS.contains(safeDecision)) {
            throw new RuntimeException(
                    "Agent 人工复核只支持 APPROVE 或 REJECT"
            );
        }

        String safeComment = trimNullable(proposedComment);

        if (safeComment != null
                && safeComment.length() > 1000) {
            throw new RuntimeException(
                    "人工复核 comment 不能超过 1000 个字符"
            );
        }

        AuditTask task =
                auditTaskService.getTaskById(taskId);

        if (!AuditTaskStatus.AUDIT_DONE.equals(
                task.getStatus())) {
            throw new RuntimeException(
                    "只有 AUDIT_DONE 状态的任务才能准备人工复核，当前状态："
                            + task.getStatus()
            );
        }

        if (!Boolean.TRUE.equals(
                task.getNeedHumanReview())) {
            throw new RuntimeException(
                    "该任务当前不需要人工复核"
            );
        }

        String dedupeKey =
                safeRequestId
                        + ":"
                        + ACTION_HUMAN_REVIEW
                        + ":"
                        + taskId
                        + ":"
                        + safeDecision;

        AgentActionRequestRecord existing =
                actionMapper.selectByDedupeKey(
                        dedupeKey
                );

        if (existing != null) {
            return existing;
        }

        LocalDateTime now = LocalDateTime.now();

        AgentActionRequestRecord record =
                new AgentActionRequestRecord();

        record.setActionToken(
                "ACT-" + UUID.randomUUID()
        );
        record.setDedupeKey(dedupeKey);
        record.setRequestId(safeRequestId);
        record.setActionType(ACTION_HUMAN_REVIEW);
        record.setTaskId(taskId);
        record.setDecision(safeDecision);
        record.setProposedComment(safeComment);
        record.setStatus(PENDING_CONFIRMATION);
        record.setCreatedAt(now);
        record.setExpiresAt(
                now.plusMinutes(confirmationTtlMinutes)
        );

        int rows = actionMapper.insertAction(record);

        if (rows != 1) {
            throw new RuntimeException(
                    "创建 Agent 待确认动作失败"
            );
        }

        return record;
    }

    @Override
    @Transactional
    public AgentActionExecutionResult confirmHumanReview(
            String actionToken,
            AgentActionConfirmRequest request) {

        ensureEnabled();

        String safeToken =
                requireText(
                        actionToken,
                        "confirmationToken 不能为空"
                );

        if (request == null) {
            throw new RuntimeException(
                    "确认参数不能为空"
            );
        }

        String confirmedBy =
                requireText(
                        request.getConfirmedBy(),
                        "confirmedBy 不能为空"
                );

        if (confirmedBy.length() > 128) {
            throw new RuntimeException(
                    "confirmedBy 不能超过 128 个字符"
            );
        }

        String confirmComment =
                trimNullable(request.getComment());

        if (confirmComment != null
                && confirmComment.length() > 1000) {
            throw new RuntimeException(
                    "comment 不能超过 1000 个字符"
            );
        }

        AgentActionRequestRecord action =
                actionMapper.selectByTokenForUpdate(
                        safeToken
                );

        if (action == null) {
            throw new RuntimeException(
                    "Agent 待确认动作不存在"
            );
        }

        if (EXECUTED.equals(action.getStatus())) {
            return new AgentActionExecutionResult(
                    AgentPendingActionView.from(action),
                    auditTaskService.getTaskDetail(
                            action.getTaskId()
                    ),
                    false,
                    "该 confirmationToken 已执行过，本次未重复执行业务写操作"
            );
        }

        if (CANCELLED.equals(action.getStatus())) {
            return new AgentActionExecutionResult(
                    AgentPendingActionView.from(action),
                    null,
                    false,
                    "该待确认动作已经取消"
            );
        }

        LocalDateTime now = LocalDateTime.now();

        if (EXPIRED.equals(action.getStatus())
                || action.getExpiresAt() == null
                || now.isAfter(action.getExpiresAt())) {

            if (PENDING_CONFIRMATION.equals(
                    action.getStatus())) {
                actionMapper.updateStatusIfPending(
                        action.getId(),
                        EXPIRED
                );
                action.setStatus(EXPIRED);
            }

            return new AgentActionExecutionResult(
                    AgentPendingActionView.from(action),
                    null,
                    false,
                    "该 confirmationToken 已过期，请重新发起 Agent 请求"
            );
        }

        if (!PENDING_CONFIRMATION.equals(
                action.getStatus())) {
            throw new RuntimeException(
                    "当前 Agent 动作状态不能执行："
                            + action.getStatus()
            );
        }

        if (!ACTION_HUMAN_REVIEW.equals(
                action.getActionType())) {
            throw new RuntimeException(
                    "不支持的 Agent 动作类型："
                            + action.getActionType()
            );
        }

        HumanReviewRequest humanReviewRequest =
                new HumanReviewRequest();

        humanReviewRequest.setDecision(
                action.getDecision()
        );
        humanReviewRequest.setReviewer(
                confirmedBy
        );
        humanReviewRequest.setComment(
                confirmComment == null
                        ? action.getProposedComment()
                        : confirmComment
        );

        AuditTaskDetailResult detail =
                auditTaskService.humanReview(
                        action.getTaskId(),
                        humanReviewRequest
                );

        String finalDecision =
                detail.getAuditTask() == null
                        ? "UNKNOWN"
                        : detail.getAuditTask()
                        .getFinalDecision();

        int rows = actionMapper.updateExecuted(
                action.getId(),
                confirmedBy,
                "finalDecision=" + finalDecision,
                now
        );

        if (rows != 1) {
            throw new RuntimeException(
                    "更新 Agent 动作执行状态失败"
            );
        }

        action.setStatus(EXECUTED);
        action.setConfirmedBy(confirmedBy);
        action.setExecutedAt(now);
        action.setResultSummary(
                "finalDecision=" + finalDecision
        );

        return new AgentActionExecutionResult(
                AgentPendingActionView.from(action),
                detail,
                true,
                "用户确认完成，业务写操作已通过现有 Workflow / State Machine 执行"
        );
    }

    @Override
    public AgentPendingActionView getAction(
            String actionToken) {

        String safeToken =
                requireText(
                        actionToken,
                        "confirmationToken 不能为空"
                );

        AgentActionRequestRecord action =
                actionMapper.selectByToken(safeToken);

        if (action == null) {
            throw new RuntimeException(
                    "Agent 待确认动作不存在"
            );
        }

        if (PENDING_CONFIRMATION.equals(
                action.getStatus())
                && action.getExpiresAt() != null
                && LocalDateTime.now()
                .isAfter(action.getExpiresAt())) {
            action.setStatus(EXPIRED);
        }

        return AgentPendingActionView.from(action);
    }

    @Override
    @Transactional
    public AgentPendingActionView cancelAction(
            String actionToken) {

        String safeToken =
                requireText(
                        actionToken,
                        "confirmationToken 不能为空"
                );

        AgentActionRequestRecord action =
                actionMapper.selectByTokenForUpdate(
                        safeToken
                );

        if (action == null) {
            throw new RuntimeException(
                    "Agent 待确认动作不存在"
            );
        }

        if (!PENDING_CONFIRMATION.equals(
                action.getStatus())) {
            return AgentPendingActionView.from(action);
        }

        String targetStatus =
                action.getExpiresAt() != null
                        && LocalDateTime.now()
                        .isAfter(action.getExpiresAt())
                        ? EXPIRED
                        : CANCELLED;

        int rows = actionMapper.updateStatusIfPending(
                action.getId(),
                targetStatus
        );

        if (rows != 1) {
            throw new RuntimeException(
                    "取消 Agent 待确认动作失败"
            );
        }

        action.setStatus(targetStatus);

        return AgentPendingActionView.from(action);
    }

    @Override
    public void cancelPendingByRequestId(
            String requestId) {

        if (requestId == null
                || requestId.trim().isEmpty()) {
            return;
        }

        actionMapper.cancelPendingByRequestId(
                requestId.trim()
        );
    }

    private void ensureEnabled() {

        if (!humanReviewActionEnabled) {
            throw new RuntimeException(
                    "Agent 人工复核动作未启用。请设置 AGENT_HUMAN_REVIEW_ACTION_ENABLED=true"
            );
        }
    }

    private String requireText(
            String value,
            String errorMessage) {

        if (value == null
                || value.trim().isEmpty()) {
            throw new RuntimeException(errorMessage);
        }

        return value.trim();
    }

    private String trimNullable(String value) {

        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        return trimmed.isEmpty()
                ? null
                : trimmed;
    }
}
