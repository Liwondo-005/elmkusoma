package tz.elmkusoma.certificate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CertificateResponse {

    private UUID id;
    private UUID institutionId;
    private UUID templateId;
    private UUID studentId;
    private UUID issuedBy;
    private String serialNumber;
    private String certificateType;
    private String title;
    private String courseTitle;
    private String description;
    private String studentName;
    private String studentIdNumber;
    private String courseOrProgramme;
    private String instructorName;
    private String grade;
    private List<String> skills;
    private LocalDate completionDate;
    private LocalDateTime issueDate;
    private LocalDate expiryDate;
    private String status;
    private String verificationCode;
    private String verificationUrl;
    private String qrCodeUrl;
    private String revokedReason;
    private LocalDateTime revokedAt;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
