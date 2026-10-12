package tz.elmkusoma.certificate.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

@Entity
@Table(name = "certificate_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CertificateTemplate extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "template_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private TemplateType templateType;

    @Column(name = "html_content", columnDefinition = "TEXT")
    private String htmlContent;

    @Column(name = "css_content", columnDefinition = "TEXT")
    private String cssContent;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "signature_line_1")
    private String signatureLine1;

    @Column(name = "signature_line_2")
    private String signatureLine2;

    @Column(name = "signature_line_3")
    private String signatureLine3;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /** Current content version; incremented (and the previous content archived) on every edit. */
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Integer version = 1;

    /** Guard: version/active must be present for the NOT NULL columns regardless of build path. */
    @PrePersist
    void onCreate() {
        if (version == null) version = 1;
        if (isActive == null) isActive = true;
        if (templateFamily == null) templateFamily = TemplateFamily.FAMILY_TRAINING;
    }

    /**
     * Which foundational family this template belongs to.
     *
     * <p>Added by V161 and defaulting to the training family, so every pre-existing template keeps
     * working unchanged. {@code TemplateType} is left exactly as it was: it describes what a
     * certificate is for, while the family describes the document shape and how much review it
     * attracts. Both families share one entity, one lifecycle and one verification path.</p>
     */
    @Column(name = "template_family", nullable = false, length = 30)
    @Builder.Default
    private TemplateFamily templateFamily = TemplateFamily.FAMILY_TRAINING;

    public enum TemplateType {
        COMPLETION,
        ACHIEVEMENT,
        PARTICIPATION,
        TRANSCRIPT,
        CUSTOM
    }

    /**
     * FOUNDATIONAL FAMILIES.
     *
     * <p>FAMILY_ACADEMIC is for formal academic awards. It is not a claim that any qualification
     * is accredited or nationally recognised - that is a property of the issuer's authority and the
     * qualification's status, not of the template, and this system does not assert it anywhere.</p>
     */
    public enum TemplateFamily {
        /** Course or training completion, attendance and achievement documents. */
        FAMILY_TRAINING,
        /** Formal academic award documents issued by an eligible institution. */
        FAMILY_ACADEMIC
    }
}
