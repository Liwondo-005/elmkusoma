package tz.elmkusoma.certificate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CertificateVerificationResponse {

    private boolean valid;
    private String id;
    private String serialNumber;
    private String studentName;
    private String certificateType;
    private String title;
    private String courseTitle;
    private String instructorName;
    private String grade;
    private List<String> skills;
    private LocalDate completionDate;
    private String institutionName;
    private String issuedBy;
    private LocalDateTime issueDate;
    private String status;
    private String message;
}
