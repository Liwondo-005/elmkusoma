package tz.elmkusoma.administration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateSummaryResponse {
    private UUID id;
    private UUID studentId;
    private String serialNumber;
    private String certificateNumber;
    private String title;
    private String studentName;
    private String certificateType;
    private String courseOrProgramme;
    private UUID institutionId;
    private String institutionName;
    private LocalDateTime issueDate;
    private java.time.LocalDate expiryDate;
    private String status;
    private LocalDateTime createdAt;
}
