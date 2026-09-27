package tz.elmkusoma.certificate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Platform-level template view: template content + institution + usage + linked signatories. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformTemplateResponse {
    private UUID id;
    private UUID institutionId;
    private String institutionName;
    private String name;
    private String description;
    private String templateType;
    private String htmlContent;
    private String cssContent;
    private String logoUrl;
    private String signatureLine1;
    private String signatureLine2;
    private String signatureLine3;
    private Boolean isActive;
    private Integer version;
    /** Number of certificates issued against this template (real count). */
    private Long usageCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    /** Linked authorised signatories in display order. */
    private List<SignatoryResponse> signatories;
}
