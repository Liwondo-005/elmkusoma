package tz.elmkusoma.nfe.certificate.domain;

import jakarta.persistence.*;
import lombok.*;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "nfe_certificates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NfeCertificate extends BaseEntity {

    @Column(name = "provider_id", nullable = false)
    private UUID providerId;

    @Column(name = "learner_id", nullable = false)
    private UUID learnerId;

    @Column(name = "program_id")
    private UUID programId;

    @Column(name = "certificate_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private CertificateType certificateType;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "student_name", nullable = false)
    private String studentName;

    @Column(name = "serial_number")
    private String serialNumber;

    @Column(name = "verification_code")
    private String verificationCode;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private CertificateStatus status = CertificateStatus.DRAFT;

    @Column(name = "issued_by")
    private UUID issuedBy;

    public enum CertificateType {
        COMPLETION, PARTICIPATION, ACHIEVEMENT
    }

    public enum CertificateStatus {
        DRAFT, ISSUED, REVOKED
    }
}
