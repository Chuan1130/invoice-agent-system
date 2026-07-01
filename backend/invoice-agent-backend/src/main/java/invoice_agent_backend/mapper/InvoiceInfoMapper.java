package invoice_agent_backend.mapper;

import invoice_agent_backend.entity.InvoiceInfo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InvoiceInfoMapper {

    int insertInvoiceInfo(InvoiceInfo invoiceInfo);

    InvoiceInfo selectInvoiceInfoByTaskId(Long taskId);
}