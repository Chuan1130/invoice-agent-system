package invoice_agent_backend.common;

import java.util.List;

/*
 ** 通用分页返回对象
 **
 ** 例如：
 ** page = 1
 ** size = 20
 ** total = 53
 ** records = 当前第一页的 20 条任务
 */
public class PageResult<T> {

    private Integer page;
    private Integer size;
    private Long total;
    private List<T> records;

    public PageResult() {
    }

    public PageResult(Integer page,
                      Integer size,
                      Long total,
                      List<T> records) {
        this.page = page;
        this.size = size;
        this.total = total;
        this.records = records;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = records;
    }
}