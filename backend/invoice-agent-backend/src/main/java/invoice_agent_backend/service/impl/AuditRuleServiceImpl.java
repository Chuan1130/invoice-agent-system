package invoice_agent_backend.service.impl;

import invoice_agent_backend.entity.AuditRuleHit;
import invoice_agent_backend.entity.InvoiceInfo;
import invoice_agent_backend.mapper.AuditRuleHitMapper;
import invoice_agent_backend.mapper.InvoiceInfoMapper;
import invoice_agent_backend.service.AuditRuleService;
import invoice_agent_backend.vo.AuditResult;
import org.springframework.stereotype.Service;
import invoice_agent_backend.constant.AuditDecision;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/*
 ** 基础审核规则服务。
 **
 ** 当前先做三个简单规则：
 **
 ** 1. RULE_REQUIRED_FIELD
 **    必要字段校验：发票号、购买方、销售方不能为空。
 **
 ** 2. RULE_AMOUNT_LIMIT
 **    金额超限校验：金额超过 50000 元，需要人工复核。
 **
 ** 3. RULE_DUPLICATE_INVOICE
 **    重复发票校验：同一个发票号已经存在，疑似重复报销。
 **
 ** 判断逻辑：
 ** - 没命中任何规则：APPROVED
 ** - 命中至少一条规则：NEED_HUMAN_REVIEW
 */
@Service
public class AuditRuleServiceImpl implements AuditRuleService {

    private final AuditRuleHitMapper auditRuleHitMapper;
    private final InvoiceInfoMapper invoiceInfoMapper;

    public AuditRuleServiceImpl(AuditRuleHitMapper auditRuleHitMapper,
                                InvoiceInfoMapper invoiceInfoMapper) {
        this.auditRuleHitMapper = auditRuleHitMapper;
        this.invoiceInfoMapper = invoiceInfoMapper;
    }

    @Override
    public AuditResult checkRules(InvoiceInfo invoiceInfo) {
        if (invoiceInfo == null || invoiceInfo.getTaskId() == null) {
            throw new RuntimeException("发票信息不能为空");
        }

        List<AuditRuleHit> ruleHits = new ArrayList<>();

        checkRequiredFields(invoiceInfo, ruleHits);
        checkAmountLimit(invoiceInfo, ruleHits);
        checkDuplicateInvoice(invoiceInfo, ruleHits);

        for (AuditRuleHit ruleHit : ruleHits) {
            auditRuleHitMapper.insertAuditRuleHit(ruleHit);
        }

        boolean needHumanReview = !ruleHits.isEmpty();
        String finalDecision =
                needHumanReview
                        ? AuditDecision.NEED_HUMAN_REVIEW
                        : AuditDecision.APPROVED;

        return new AuditResult(finalDecision, needHumanReview, ruleHits);
    }

    /*
     ** 规则一：必要字段校验。
     ** 如果发票号、购买方、销售方为空，就需要人工复核。
     */
    private void checkRequiredFields(InvoiceInfo invoiceInfo, List<AuditRuleHit> ruleHits) {
        if (isBlank(invoiceInfo.getInvoiceNo())
                || isBlank(invoiceInfo.getBuyerName())
                || isBlank(invoiceInfo.getSellerName())) {
            ruleHits.add(buildRuleHit(
                    invoiceInfo.getTaskId(),
                    "RULE_REQUIRED_FIELD",
                    "必要字段校验",
                    "HIT",
                    "发票号、购买方或销售方存在空值，需要人工复核"
            ));
        }
    }

    /*
     ** 规则二：金额超限校验。
     ** 当前先写死阈值 50000。
     ** 之后可以改成从数据库规则表或配置文件读取。
     */
    private void checkAmountLimit(InvoiceInfo invoiceInfo, List<AuditRuleHit> ruleHits) {
        BigDecimal amount = invoiceInfo.getAmount();

        if (amount != null && amount.compareTo(new BigDecimal("50000.00")) > 0) {
            ruleHits.add(buildRuleHit(
                    invoiceInfo.getTaskId(),
                    "RULE_AMOUNT_LIMIT",
                    "金额超限校验",
                    "HIT",
                    "发票金额超过 50000 元，需要人工复核"
            ));
        }
    }

    /*
     ** 规则三：重复发票校验。
     ** 如果 invoice_info 表中已经存在相同发票号，并且不是当前任务本身，
     ** 就说明这张发票可能被重复报销。
     */
    private void checkDuplicateInvoice(InvoiceInfo invoiceInfo, List<AuditRuleHit> ruleHits) {
        if (isBlank(invoiceInfo.getInvoiceNo())) {
            return;
        }

        int duplicateCount = invoiceInfoMapper.countByInvoiceNoExcludeTask(
                invoiceInfo.getInvoiceNo(),
                invoiceInfo.getTaskId()
        );

        if (duplicateCount > 0) {
            ruleHits.add(buildRuleHit(
                    invoiceInfo.getTaskId(),
                    "RULE_DUPLICATE_INVOICE",
                    "重复发票校验",
                    "HIT",
                    "该发票号码已存在，疑似重复报销"
            ));
        }
    }

    private AuditRuleHit buildRuleHit(Long taskId,
                                      String ruleCode,
                                      String ruleName,
                                      String hitResult,
                                      String ruleMessage) {
        AuditRuleHit ruleHit = new AuditRuleHit();
        ruleHit.setTaskId(taskId);
        ruleHit.setRuleCode(ruleCode);
        ruleHit.setRuleName(ruleName);
        ruleHit.setHitResult(hitResult);
        ruleHit.setRuleMessage(ruleMessage);
        return ruleHit;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}