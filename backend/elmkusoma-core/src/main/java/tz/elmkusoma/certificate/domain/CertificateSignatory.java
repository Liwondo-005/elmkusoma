package tz.elmkusoma.certificate.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDate;

/**
 * An authorised certificate signatory.
 *
 * <p>Distinguished from authorisation: this record is the signatory PROFILE (who they are,
 * signature image, title). The authorisation fields on the same record decide whether the
 * profile may actually sign: {@code status} (ACTIVE/INACTIVE), {@code certificateTypes}
 * scope, {@code institutionId} scope (NULL = platform-wide) and the {@code validFrom}
 * / {@code validUntil} validity window.</p>
 *
 * <p>{@code institutionId} inherited from {@link BaseEntity} is the institution scope —
 * a NULL value means the signatory is platform-wide and may be linked to any
 * institution's template, subject to type/validity checks.</p>
 */
@Entity
@Table(name = "certificate_signatories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CertificateSignatory extends BaseEntity {

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(name = "position_title", length = 200)
    private String positionTitle;

    @Column(name = "organization", length = 200)
    private String organization;

    /**
     * Signature image as a self-contained data URL (data:image/...;base64,...) or an
     * https:// URL. Never a filesystem path (validated in CertificateGovernanceService).
     */
    @Column(name = "signature_image", columnDefinition = "TEXT")
    private String signatureImage;

    /** CSV of {@link Certificate.CertificateType} names this signatory may sign; NULL = all types. */
    @Column(name = "certificate_types", length = 200)
    private String certificateTypes;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private SignatoryStatus status = SignatoryStatus.ACTIVE;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    public enum SignatoryStatus {
        ACTIVE,
        INACTIVE
    }
}
