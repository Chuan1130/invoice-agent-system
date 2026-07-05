package invoice_agent_backend.mapper;

import invoice_agent_backend.entity.InvoiceInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface InvoiceInfoMapper {

    int insertInvoiceInfo(InvoiceInfo invoiceInfo);

    InvoiceInfo selectInvoiceInfoByTaskId(Long taskId);

    /*
     ** 用于重复发票检测。
     **
     ** 逻辑：
     ** 查 invoice_info 表里是否已经存在相同 invoice_no。
     ** 但要排除当前 taskId，因为当前上传这张发票本身也会占一条记录。
     */
    int countByInvoiceNoExcludeTask(@Param("invoiceNo") String invoiceNo,
                                    @Param("taskId") Long taskId);
}