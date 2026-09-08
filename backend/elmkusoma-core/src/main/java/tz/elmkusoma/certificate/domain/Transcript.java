package tz.elmkusoma.certificate.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tz.elmkusoma.common.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "transcripts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transcript extends BaseEntity {

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "issued_by", nullable = false)
    private UUID issuedBy;

    @Column(name = "serial_number", nullable = false, unique = true)
    private String serialNumber;

    @Column(name = "academic_year")
    private String academicYear;

    @Column(name = "term")
    private String term;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private TranscriptStatus status = TranscriptStatus.DRAFT;

    @Column(name = "total_subjects")
    private Integer totalSubjects;

    @Column(name = "average_score", precision = 5, scale = 2)
    private BigDecimal averageScore;

    @Column(name = "class_rank")
    private Integer classRank;

    @Column(name = "remarks")
    private String remarks;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    public enum TranscriptStatus {
        DRAFT,
        ISSUED,
        REVOKED
    }
}
