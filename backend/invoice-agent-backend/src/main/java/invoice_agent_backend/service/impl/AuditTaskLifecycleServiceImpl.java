package invoice_agent_backend.service.impl;

import invoice_agent_backend.constant.AuditTaskStatus;
import invoice_agent_backend.entity.AuditTask;
import invoice_agent_backend.mapper.AuditTaskMapper;
import invoice_agent_backend.service.AuditTaskLifecycleService;
import invoice_agent_backend.workflow.state.AuditTaskStateMachine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/*
 ** 审核任务生命周期服务实现。
 **
 ** 这里最重要的是事务边界：
 **
 ** createTask：
 ** REQUIRES_NEW
 ** 任务创建以后立即提交。
 **
 ** markOcrProcessing：
 ** REQUIRES_NEW
 ** 调用 OCR 前立即提交 OCR_PROCESSING。
 **
 ** markFailed：
 ** REQUIRES_NEW
 ** 即使主 Workflow 已经回滚，
 ** FAILED 仍然可以单独保存。
 **
 ** transitionStatus：
 ** 使用默认 REQUIRED。
 ** 如果外层已经有主事务，就加入主事务。
 */
@Service
public class AuditTaskLifecycleServiceImpl
        implements AuditTaskLifecycleService {

    private final AuditTaskMapper auditTaskMapper;

    private final AuditTaskStateMachine
            auditTaskStateMachine;

    public AuditTaskLifecycleServiceImpl(
            AuditTaskMapper auditTaskMapper,
            AuditTaskStateMachine auditTaskStateMachine) {

        this.auditTaskMapper =
                auditTaskMapper;

        this.auditTaskStateMachine =
                auditTaskStateMachine;
    }

    /*
     ** 独立事务一：
     ** 创建 audit_task。
     **
     ** 即使后续 OCR 失败，
     ** 这条任务也不会被主事务一起回滚。
     */
    @Override
    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public AuditTask createTask(
            String originalFilePath) {

        if (originalFilePath == null
                || originalFilePath
                .trim()
                .isEmpty()) {

            throw new RuntimeException(
                    "原始发票文件路径不能为空"
            );
        }

        String taskNo =
                "TASK-" + System.currentTimeMillis();

        AuditTask auditTask =
                new AuditTask();

        auditTask.setTaskNo(taskNo);

        auditTask.setUserId(null);

        auditTask.setStatus(
                AuditTaskStatus.UPLOADED
        );

        auditTask.setOriginalFilePath(
                originalFilePath
        );

        auditTask.setOcrRawText(null);

        auditTask.setFinalDecision(null);

        auditTask.setNeedHumanReview(false);

        auditTask.setReportPath(null);

        int affectedRows =
                auditTaskMapper
                        .insertAuditTask(auditTask);

        if (affectedRows != 1) {
            throw new RuntimeException(
                    "创建审核任务失败"
            );
        }

        if (auditTask.getId() == null) {
            throw new RuntimeException(
                    "创建审核任务后没有获得任务ID"
            );
        }

        return auditTask;
    }

    /*
     ** 独立事务二：
     **
     ** UPLOADED -> OCR_PROCESSING
     **
     ** 该事务会在真正调用百度 OCR 前提交。
     */
    @Override
    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public void markOcrProcessing(
            Long taskId) {

        transitionStatusInternal(
                taskId,
                AuditTaskStatus.OCR_PROCESSING
        );
    }

    /*
     ** 独立事务三：
     **
     ** Workflow 失败后记录 FAILED。
     **
     ** 即使 Core Workflow 的主事务已经回滚，
     ** 这里仍然会开启一个新事务。
     */
    @Override
    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public void markFailed(
            Long taskId) {

        transitionStatusInternal(
                taskId,
                AuditTaskStatus.FAILED
        );
    }

    /*
     ** 普通状态转换。
     **
     ** 默认 Propagation.REQUIRED：
     **
     ** 如果调用者已经有事务，
     ** 就加入调用者的事务。
     **
     ** 后面 Core Workflow 会通过这个方法完成：
     **
     ** OCR_PROCESSING -> OCR_DONE
     ** OCR_DONE -> AUDIT_DONE
     ** OCR_DONE -> COMPLETED
     */
    @Override
    @Transactional
    public void transitionStatus(
            Long taskId,
            String targetStatus) {

        transitionStatusInternal(
                taskId,
                targetStatus
        );
    }

    /*
     ** 真正执行状态转换的内部方法。
     **
     ** 步骤：
     **
     ** 1. 查询任务
     ** 2. 取得数据库当前状态
     ** 3. 通过状态机检查
     ** 4. 使用带旧状态条件的 SQL 更新
     ** 5. 检查更新行数
     */
    private void transitionStatusInternal(
            Long taskId,
            String targetStatus) {

        if (taskId == null) {
            throw new RuntimeException(
                    "任务ID不能为空"
            );
        }

        AuditTask auditTask =
                auditTaskMapper
                        .selectAuditTaskById(taskId);

        if (auditTask == null) {
            throw new RuntimeException(
                    "审核任务不存在，taskId："
                            + taskId
            );
        }

        String currentStatus =
                auditTask.getStatus();

        auditTaskStateMachine
                .validateTransition(
                        currentStatus,
                        targetStatus
                );

        int affectedRows =
                auditTaskMapper
                        .updateTaskStatus(
                                taskId,
                                currentStatus,
                                targetStatus
                        );

        /*
         ** 如果更新行数不是 1，
         ** 说明查询以后状态可能又被其他请求修改了。
         */
        if (affectedRows != 1) {
            throw new RuntimeException(
                    "任务状态更新失败，"
                            + "可能发生了并发状态修改："
                            + currentStatus
                            + " -> "
                            + targetStatus
            );
        }
    }
}