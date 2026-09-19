package tz.elmkusoma.administration.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "data_import_jobs")
@Getter
@Setter
@NoArgsConstructor
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

    public static DataImportJob of(UUID importedBy, String importType, String fileName,
                                   String fileUrl, UUID institutionId) {
        DataImportJob job = new DataImportJob();
        job.setImportedBy(importedBy);
        job.setImportType(importType);
        job.setFileName(fileName);
        job.setFileUrl(fileUrl);
        job.setInstitutionId(institutionId);
        job.setStatus(ImportStatus.PENDING);
        job.setTotalRows(0);
        job.setProcessedRows(0);
        job.setSuccessfulRows(0);
        job.setFailedRows(0);
        job.setStartedAt(LocalDateTime.now());
        return job;
    }
}
