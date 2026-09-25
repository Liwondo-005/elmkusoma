package tz.elmkusoma.certificate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/** An archived certificate template version (previous content kept in coexistence). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateVersionResponse {
    private UUID id;
    private UUID templateId;
    private Integer version;
    private String name;
    private String templateType;
    private String description;
    private String htmlContent;
    private String cssContent;
    private String logoUrl;
    private LocalDateTime archivedAt;
    private String archivedBy;
}
