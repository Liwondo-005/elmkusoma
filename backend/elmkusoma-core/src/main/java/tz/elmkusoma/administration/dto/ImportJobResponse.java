package tz.elmkusoma.administration.dto;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public class ImportJobResponse {

    private UUID id;
    private UUID institutionId;
    private UUID importedBy;
    private String importType;
    private String fileName;
    private String status;
    private Integer totalRows;
    private Integer processedRows;
    private Integer successfulRows;
    private Integer failedRows;
    private Map<String, Object> errorLog;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getInstitutionId() { return institutionId; }
    public void setInstitutionId(UUID institutionId) { this.institutionId = institutionId; }
    public UUID getImportedBy() { return importedBy; }
    public void setImportedBy(UUID importedBy) { this.importedBy = importedBy; }
    public String getImportType() { return importType; }
    public void setImportType(String importType) { this.importType = importType; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getTotalRows() { return totalRows; }
    public void setTotalRows(Integer totalRows) { this.totalRows = totalRows; }
    public Integer getProcessedRows() { return processedRows; }
    public void setProcessedRows(Integer processedRows) { this.processedRows = processedRows; }
    public Integer getSuccessfulRows() { return successfulRows; }
    public void setSuccessfulRows(Integer successfulRows) { this.successfulRows = successfulRows; }
    public Integer getFailedRows() { return failedRows; }
    public void setFailedRows(Integer failedRows) { this.failedRows = failedRows; }
    public Map<String, Object> getErrorLog() { return errorLog; }
    public void setErrorLog(Map<String, Object> errorLog) { this.errorLog = errorLog; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static ImportJobResponse of(UUID id, UUID institutionId, UUID importedBy,
                                       String importType, String fileName, String status,
                                       Integer totalRows, Integer processedRows,
                                       Integer successfulRows, Integer failedRows,
                                       Map<String, Object> errorLog,
                                       LocalDateTime startedAt, LocalDateTime completedAt,
                                       LocalDateTime createdAt) {
        ImportJobResponse resp = new ImportJobResponse();
        resp.id = id;
        resp.institutionId = institutionId;
        resp.importedBy = importedBy;
        resp.importType = importType;
        resp.fileName = fileName;
        resp.status = status;
        resp.totalRows = totalRows;
        resp.processedRows = processedRows;
        resp.successfulRows = successfulRows;
        resp.failedRows = failedRows;
        resp.errorLog = errorLog;
        resp.startedAt = startedAt;
        resp.completedAt = completedAt;
        resp.createdAt = createdAt;
        return resp;
    }
}