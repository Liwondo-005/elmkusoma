package tz.elmkusoma.certificate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tz.elmkusoma.certificate.domain.CertificateTemplate.TemplateType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTemplateRequest {

    @NotBlank(message = "Template name is required")
    private String name;

    private String description;

    @NotNull(message = "Template type is required")
    private TemplateType templateType;

    private String htmlContent;

    private String cssContent;

    private String logoUrl;

    private String signatureLine1;

    private String signatureLine2;

    private String signatureLine3;
}
