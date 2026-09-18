package tz.elmkusoma.nfe.certificate.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NfeCertificateRequest {

    private UUID providerId;

    private UUID learnerId;

    private UUID programId;

    @NotBlank(message = "Certificate type is required")
    private String certificateType;

    @NotBlank(message = "Certificate title is required")
    private String title;

    @NotBlank(message = "Student name is required")
    private String studentName;

    private String serialNumber;

    private String verificationCode;

    private LocalDateTime issuedAt;

    private LocalDate expiryDate;

    private String status;

    private UUID issuedBy;
}
