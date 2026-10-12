package tz.elmkusoma.certificate.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tz.elmkusoma.common.BaseEntity;
import tz.elmkusoma.certificate.qr.CertificateQrCode;

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

    /**
     * Legacy NOT NULL column in the certificates table (unique). Not previously mapped,
     * which caused every certificate INSERT to fail with a not-null constraint violation.
     */
    @Column(name = "certificate_number")
    private String certificateNumber;

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
    @Builder.Default
    private CertificateStatus status = CertificateStatus.DRAFT;

    @Column(name = "verification_code", nullable = false, unique = true)
    private String verificationCode;

    @Column(name = "verification_url")
    private String verificationUrl;

    /**
     * The QR the certificate carries, as a self-contained SVG data URI.
     *
     * <p>{@code text} rather than the JPA default of varchar(255): a stored code is a few
     * kilobytes, and Hibernate's default would create a column far too small to hold one. The
     * database column was widened to text by V163 for the same reason. (Production runs
     * ddl-auto=none, but the test profile uses create-drop, so the declared type has to be right
     * here too or every issuance in a test would fail on a length overflow.)</p>
     */
    @Column(name = "qr_code_url", columnDefinition = "text")
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

    /**
     * certificate_number is NOT NULL + UNIQUE in the database but has no default,
     * so every INSERT must provide it. Generate one automatically when the caller
     * did not set it explicitly.
     *
     * <p>The QR is stamped here rather than at each issuance site. There are four - the manual
     * service, live-class expiry, event attendance and NFE - and any one of them forgetting to
     * call back would produce certificates that scan and certificates that do not, indistinguishable
     * until somebody tries. A lifecycle callback cannot be skipped.</p>
     */
    @PrePersist
    void onCreate() {
        if (certificateNumber == null || certificateNumber.isBlank()) {
            certificateNumber = "CERT-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        }
        // Only when there is something to encode, and only when a code is not already set, so a
        // caller that generated one deliberately keeps it.
        if (qrCodeUrl == null && verificationUrl != null && !verificationUrl.isBlank()) {
            qrCodeUrl = CertificateQrCode.svgDataUri(verificationUrl);
        }
    }

    public enum CertificateType {
        COMPLETION,
        ACHIEVEMENT,
        PARTICIPATION,
        TRANSCRIPT
    }

    /**
     * The authoritative lifecycle of a certificate.
     *
     * <p>Extended additively by V161. There is deliberately no second state machine: DRAFT,
     * PENDING_APPROVAL and APPROVED sit in the same column as ISSUED and REVOKED so a certificate
     * always has exactly one lifecycle field.</p>
     *
     * <p>Existing rows are unaffected - the column was an unconstrained varchar, and the new
     * values are only ever written by the approval workflow.</p>
     */
    public enum CertificateStatus {
        /** Prepared, not yet submitted for review. */
        DRAFT,
        /** Submitted; at least one required approval stage is still outstanding. */
        PENDING_APPROVAL,
        /** Every stage the policy requires is approved. Issuable. */
        APPROVED,
        /** Released to the holder. */
        ISSUED,
        /** Withdrawn after issuance. */
        REVOKED
    }
}
