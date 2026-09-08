package tz.elmkusoma.certificate.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "certificates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Certificate extends BaseEntity {

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "issued_by", nullable = false)
    private UUID issuedBy;

    @Column(name = "serial_number", nullable = false, unique = true)
    private String serialNumber;

    @Column(name = "certificate_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private CertificateType certificateType;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "student_name", nullable = false)
    private String studentName;

    @Column(name = "student_id_number")
    private String studentIdNumber;

    @Column(name = "course_or_programme")
    private String courseOrProgramme;

    @Column(name = "completion_date", nullable = false)
    private LocalDate completionDate;

    @Column(name = "issue_date", nullable = false)
    private LocalDateTime issueDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private CertificateStatus status = CertificateStatus.DRAFT;

    @Column(name = "verification_code", nullable = false, unique = true)
    private String verificationCode;

    @Column(name = "verification_url")
    private String verificationUrl;

    @Column(name = "qr_code_url")
    private String qrCodeUrl;

    @Column(name = "instructor_name")
    private String instructorName;

    @Column(name = "grade")
    private String grade;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "skills", columnDefinition = "jsonb")
    private List<String> skills;

    @Column(name = "revoked_reason")
    private String revokedReason;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    public enum CertificateType {
        COMPLETION,
        ACHIEVEMENT,
        PARTICIPATION,
        TRANSCRIPT
    }

    public enum CertificateStatus {
        DRAFT,
        ISSUED,
        REVOKED
    }
}
