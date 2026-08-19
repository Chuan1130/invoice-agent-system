package invoice_agent_backend.service;

import invoice_agent_backend.entity.AuditTask;

/*
 ** 审核任务生命周期服务。
 **
 ** 主要负责：
 **
 ** 1. 使用独立事务创建审核任务
 ** 2. 使用独立事务记录 OCR_PROCESSING
 ** 3. 使用独立事务记录 FAILED
 ** 4. 在主业务事务中执行普通状态转换
 */
public interface AuditTaskLifecycleService {

    /*
     ** 创建任务并立即提交。
     **
     ** 返回的 AuditTask 已经包含数据库生成的 id。
     */
    AuditTask createTask(String originalFilePath);

    /*
     ** 开始 OCR 前调用。
     **
     ** UPLOADED -> OCR_PROCESSING
     */
    void markOcrProcessing(Long taskId);

    /*
     ** Workflow 发生异常后调用。
     **
     ** 当前非终态 -> FAILED
     */
    void markFailed(Long taskId);

    /*
     ** 在 OCR、规则审核和人工审核的主事务中，
     ** 执行普通状态转换。
     */
    void transitionStatus(
            Long taskId,
            String targetStatus
    );
}