package tz.elmkusoma.certificate.domain;

import jakarta.persistence.*;
import lombok.*;
import tz.elmkusoma.common.BaseEntity;

@Entity
@Table(name = "certificate_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
    private Boolean isActive = true;

    public enum TemplateType {
        COMPLETION,
        ACHIEVEMENT,
        PARTICIPATION,
        TRANSCRIPT,
        CUSTOM
    }
}
