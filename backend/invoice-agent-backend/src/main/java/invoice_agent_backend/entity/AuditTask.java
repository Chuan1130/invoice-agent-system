package invoice_agent_backend.entity;

import java.time.LocalDateTime;

public class AuditTask {

    private Long id;
    private String taskNo;
    private Long userId;
    private String status;
    private String originalFilePath;
    private String ocrRawText;
    private String finalDecision;
    private Boolean needHumanReview;
    private String reportPath;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTaskNo() {
        return taskNo;
    }

    public void setTaskNo(String taskNo) {
        this.taskNo = taskNo;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public String getOcrRawText() {
        return ocrRawText;
    }

    public void setOcrRawText(String ocrRawText) {
        this.ocrRawText = ocrRawText;
    }

    public String getFinalDecision() {
        return finalDecision;
    }

    public void setFinalDecision(String finalDecision) {
        this.finalDecision = finalDecision;
    }

    public Boolean getNeedHumanReview() {
        return needHumanReview;
    }

    public void setNeedHumanReview(Boolean needHumanReview) {
        this.needHumanReview = needHumanReview;
    }

    public String getReportPath() {
        return reportPath;
    }

    public void setReportPath(String reportPath) {
        this.reportPath = reportPath;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}