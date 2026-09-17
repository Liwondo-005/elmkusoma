package tz.elmkusoma.administration.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "data_import_jobs")
public class DataImportJob extends BaseEntity {

    @Column(name = "imported_by", nullable = false)
    private UUID importedBy;

    @Column(name = "import_type", nullable = false)
    private String importType;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ImportStatus status = ImportStatus.PENDING;

    @Column(name = "total_rows")
    private Integer totalRows = 0;

    @Column(name = "processed_rows")
    private Integer processedRows = 0;

    @Column(name = "successful_rows")
    private Integer successfulRows = 0;

    @Column(name = "failed_rows")
    private Integer failedRows = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "error_log", columnDefinition = "jsonb")
    private Map<String, Object> errorLog;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public enum ImportStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED
    }

    public UUID getId() { return super.getId(); }
    public UUID getInstitutionId() { return super.getInstitutionId(); }
    public void setInstitutionId(UUID institutionId) { super.setInstitutionId(institutionId); }

    public UUID getImportedBy() { return importedBy; }
    public void setImportedBy(UUID importedBy) { this.importedBy = importedBy; }
    public String getImportType() { return importType; }
    public void setImportType(String importType) { this.importType = importType; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public ImportStatus getStatus() { return status; }
    public void setStatus(ImportStatus status) { this.status = status; }
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

    public enum ImportStatus {
        PENDING, PROCESSING, COMPLETED, FAILED
    }

    public static DataImportJob of(UUID importedBy, String importType, String fileName,
                                   String fileUrl, UUID institutionId) {
        DataImportJob job = new DataImportJob();
        job.importedBy = importedBy;
        job.importType = importType;
        job.fileName = fileName;
        job.fileUrl = fileUrl;
        job.institutionId = institutionId;
        job.status = ImportStatus.PENDING;
        job.totalRows = 0;
        job.processedRows = 0;
        job.successfulRows = 0;
        job.failedRows = 0;
        job.startedAt = java.time.LocalDateTime.now();
        return job;
    }
}