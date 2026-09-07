package tz.elmkusoma.certificate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tz.elmkusoma.certificate.domain.Certificate.CertificateType;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenerateCertificateRequest {

    @NotNull(message = "Template ID is required")
    private UUID templateId;

    @NotNull(message = "Student ID is required")
    private UUID studentId;

    @NotNull(message = "Certificate type is required")
    private CertificateType certificateType;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotBlank(message = "Student name is required")
    private String studentName;

    private String studentIdNumber;

    private String courseOrProgramme;

    private String instructorName;

    private String grade;

    private List<String> skills;

    @NotNull(message = "Completion date is required")
    private LocalDate completionDate;

    private LocalDate expiryDate;
}
