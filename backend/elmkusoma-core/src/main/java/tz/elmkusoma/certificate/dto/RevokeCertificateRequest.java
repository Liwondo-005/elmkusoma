package tz.elmkusoma.certificate.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevokeCertificateRequest {

    @NotBlank(message = "Revocation reason is required")
    private String reason;
}
