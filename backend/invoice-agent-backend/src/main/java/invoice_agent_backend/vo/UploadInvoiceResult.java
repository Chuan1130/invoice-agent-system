package invoice_agent_backend.vo;

public class UploadInvoiceResult {

    private Long taskId;
    private String taskNo;
    private String status;
    private String originalFilePath;

    public UploadInvoiceResult() {
    }

    public UploadInvoiceResult(Long taskId, String taskNo, String status, String originalFilePath) {
        this.taskId = taskId;
        this.taskNo = taskNo;
        this.status = status;
        this.originalFilePath = originalFilePath;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getTaskNo() {
        return taskNo;
    }

    public void setTaskNo(String taskNo) {
        this.taskNo = taskNo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOriginalFilePath() {
        return originalFilePath;
    }

    public void setOriginalFilePath(String originalFilePath) {
        this.originalFilePath = originalFilePath;
    }
}