package tz.elmkusoma.certificate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/** Full platform-level certificate detail: certificate + issuer + template + signatories. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateDetailResponse {
    private CertificateResponse certificate;
    private String institutionName;
    private UUID templateId;
    private String templateName;
    private Integer templateVersion;
    private Boolean templateIsActive;
    private List<SignatoryResponse> signatories;
}
