package tz.elmkusoma.certificate.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * The signatory as approved at the moment a particular certificate was issued.
 *
 * <p>A certificate referenced the live {@code certificate_signatories} row. Editing that profile
 * - changing a name, a title, or the signature image - therefore silently rewrote the appearance
 * of documents issued years earlier. This is the copy that makes an issued certificate stable.</p>
 */
@Entity
@Table(name = "certificate_signatory_snapshots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CertificateSignatorySnapshot extends BaseEntity {

    @Column(name = "certificate_id", nullable = false)
    private UUID certificateId;

    /** Null once the signatory profile is deleted; the snapshot itself still stands. */
    @Column(name = "signatory_id")
    private UUID signatoryId;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(name = "position_title", length = 200)
    private String positionTitle;

    @Column(name = "organization", length = 200)
    private String organization;

    @Column(name = "signature_image", columnDefinition = "TEXT")
    private String signatureImage;

    @Column(name = "template_version")
    private Integer templateVersion;

    @Column(name = "captured_at", nullable = false)
    private LocalDateTime capturedAt;

    @PrePersist
    void stamp() {
        if (capturedAt == null) {
            capturedAt = LocalDateTime.now();
        }
        if (displayOrder == null) {
            displayOrder = 0;
        }
    }
}
