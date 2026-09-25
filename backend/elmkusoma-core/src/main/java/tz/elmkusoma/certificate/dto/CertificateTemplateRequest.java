package tz.elmkusoma.certificate.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Create/update payload for a certificate template (platform governance). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateTemplateRequest {
    @Size(max = 200)
    private String name;
    private String description;
    /** COMPLETION, ACHIEVEMENT, PARTICIPATION, TRANSCRIPT or CUSTOM. */
    private String templateType;
    private String htmlContent;
    private String cssContent;
    @Size(max = 500)
    private String logoUrl;
    @Size(max = 500)
    private String signatureLine1;
    @Size(max = 500)
    private String signatureLine2;
    @Size(max = 500)
    private String signatureLine3;
    /** Institution that owns the template (create only; never changed on update). */
    private UUID institutionId;
}
