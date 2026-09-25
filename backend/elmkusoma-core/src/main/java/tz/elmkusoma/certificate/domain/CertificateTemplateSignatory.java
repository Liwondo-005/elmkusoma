package tz.elmkusoma.certificate.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.util.UUID;

/**
 * Ordered link between a certificate template and an authorised signatory.
 * A template may carry multiple signatories; each link keeps its display order.
 */
@Entity
@Table(name = "certificate_template_signatories",
        uniqueConstraints = @UniqueConstraint(name = "uq_cert_tpl_sign", columnNames = {"template_id", "signatory_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CertificateTemplateSignatory extends BaseEntity {

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Column(name = "signatory_id", nullable = false)
    private UUID signatoryId;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;
}
