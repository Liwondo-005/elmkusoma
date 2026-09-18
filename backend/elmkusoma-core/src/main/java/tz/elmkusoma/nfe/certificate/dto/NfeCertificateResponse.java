package tz.elmkusoma.nfe.certificate.dto;

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
public class NfeCertificateResponse {

    private UUID id;
    private UUID institutionId;
    private UUID providerId;
    private UUID learnerId;
    private UUID programId;
    private String certificateType;
    private String title;
    private String studentName;
    private String serialNumber;
    private String verificationCode;
    private LocalDateTime issuedAt;
    private LocalDate expiryDate;
    private String status;
    private UUID issuedBy;
    private LocalDateTime createdAt;
}
