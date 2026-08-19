package invoice_agent_backend.workflow.state;

import invoice_agent_backend.constant.AuditTaskStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/*
 ** 审核任务状态机。
 **
 ** 这个类只负责一件事：
 ** 判断某一次状态转换是否合法。
 **
 ** 它不直接操作数据库。
 */
@Component
public class AuditTaskStateMachine {

    /*
     ** key：
     ** 当前状态
     **
     ** value：
     ** 当前状态允许进入的目标状态
     */
    private static final Map<String, Set<String>>
            ALLOWED_TRANSITIONS =
            Map.of(
                    AuditTaskStatus.UPLOADED,
                    Set.of(
                            AuditTaskStatus.OCR_PROCESSING,
                            AuditTaskStatus.FAILED
                    ),

                    AuditTaskStatus.OCR_PROCESSING,
                    Set.of(
                            AuditTaskStatus.OCR_DONE,
                            AuditTaskStatus.FAILED
                    ),

                    AuditTaskStatus.OCR_DONE,
                    Set.of(
                            AuditTaskStatus.AUDIT_DONE,
                            AuditTaskStatus.COMPLETED,
                            AuditTaskStatus.FAILED
                    ),

                    AuditTaskStatus.AUDIT_DONE,
                    Set.of(
                            AuditTaskStatus.COMPLETED,
                            AuditTaskStatus.FAILED
                    ),

                    AuditTaskStatus.COMPLETED,
                    Set.of(),

                    AuditTaskStatus.FAILED,
                    Set.of()
            );

    /*
     ** 检查状态转换是否合法。
     **
     ** 不合法时直接抛出异常，
     ** 防止错误状态被写进数据库。
     */
    public void validateTransition(
            String currentStatus,
            String targetStatus) {

        if (currentStatus == null
                || currentStatus.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "当前任务状态不能为空"
            );
        }

        if (targetStatus == null
                || targetStatus.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "目标任务状态不能为空"
            );
        }

        Set<String> allowedTargets =
                ALLOWED_TRANSITIONS.get(
                        currentStatus
                );

        if (allowedTargets == null) {
            throw new IllegalStateException(
                    "未知的任务状态：" + currentStatus
            );
        }

        if (!allowedTargets.contains(targetStatus)) {
            throw new IllegalStateException(
                    "不允许的任务状态转换："
                            + currentStatus
                            + " -> "
                            + targetStatus
            );
        }
    }

    /*
     ** 不抛异常，只返回 true 或 false。
     */
    public boolean canTransition(
            String currentStatus,
            String targetStatus) {

        if (currentStatus == null
                || targetStatus == null) {
            return false;
        }

        return ALLOWED_TRANSITIONS
                .getOrDefault(
                        currentStatus,
                        Set.of()
                )
                .contains(targetStatus);
    }

    /*
     ** 判断任务是否已经进入终态。
     **
     ** 终态不允许继续流转。
     */
    public boolean isTerminalStatus(
            String status) {

        return AuditTaskStatus.COMPLETED
                .equals(status)
                || AuditTaskStatus.FAILED
                .equals(status);
    }
}