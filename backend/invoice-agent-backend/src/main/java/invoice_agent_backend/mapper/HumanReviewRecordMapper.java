package invoice_agent_backend.mapper;

import invoice_agent_backend.entity.HumanReviewRecord;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface HumanReviewRecordMapper {

    int insertHumanReviewRecord(HumanReviewRecord record);

    List<HumanReviewRecord> selectByTaskId(Long taskId);
}