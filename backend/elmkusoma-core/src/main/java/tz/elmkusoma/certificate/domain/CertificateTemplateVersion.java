package tz.elmkusoma.certificate.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Immutable archive of a certificate template's content at a given version.
 * Written whenever the live template is edited, so previous versions continue to
 * coexist (v1, v2, ...) instead of being overwritten.
 */
@Entity
@Table(name = "certificate_template_versions",
        uniqueConstraints = @UniqueConstraint(name = "uq_cert_tpl_ver", columnNames = {"template_id", "version"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CertificateTemplateVersion extends BaseEntity {

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "template_type", nullable = false, length = 30)
    private String templateType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "html_content", columnDefinition = "TEXT")
    private String htmlContent;

    @Column(name = "css_content", columnDefinition = "TEXT")
    private String cssContent;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "archived_at", nullable = false)
    private LocalDateTime archivedAt;

    @Column(name = "archived_by")
    private String archivedBy;
}
