package tz.elmkusoma.certificate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateResponse {

    private UUID id;
    private UUID institutionId;
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
