package invoice_agent_backend.service.impl;

import invoice_agent_backend.entity.InvoiceInfo;
import invoice_agent_backend.service.OcrService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;


/*这个类现在是假的 OCR。
它不真正识别图片，只是固定返回一组测试发票字段。*/
@Service
public class MockOcrServiceImpl implements OcrService {

    @Override
    public InvoiceInfo recognizeInvoice(Long taskId, String filePath) {
        InvoiceInfo invoiceInfo = new InvoiceInfo();

        invoiceInfo.setTaskId(taskId);
        invoiceInfo.setInvoiceNo("87654321");
        invoiceInfo.setInvoiceDate(LocalDateTime.of(2025, 5, 20, 0, 0));
        invoiceInfo.setAmount(new BigDecimal("56500.00"));
        invoiceInfo.setTaxAmount(new BigDecimal("3390.00"));
        invoiceInfo.setBuyerName("杭州智联科技有限公司");
        invoiceInfo.setSellerName("上海云创信息技术有限公司");
        invoiceInfo.setInvoiceType("增值税电子普通发票");

        String rawJson = """
                {
                  "mock": true,
                  "filePath": "%s",
                  "invoiceNo": "87654321",
                  "invoiceDate": "2025-05-20",
                  "amount": 56500.00,
                  "taxAmount": 3390.00,
                  "buyerName": "杭州智联科技有限公司",
                  "sellerName": "上海云创信息技术有限公司",
                  "invoiceType": "增值税电子普通发票"
                }
                """.formatted(filePath.replace("\\", "\\\\"));

        invoiceInfo.setRawJson(rawJson);

        return invoiceInfo;
    }
}