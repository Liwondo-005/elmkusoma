package tz.elmkusoma.certificate.mapper;

import org.springframework.stereotype.Component;
import tz.elmkusoma.certificate.domain.Certificate;
import tz.elmkusoma.certificate.domain.CertificateTemplate;
import tz.elmkusoma.certificate.domain.Transcript;
import tz.elmkusoma.certificate.domain.TranscriptEntry;
import tz.elmkusoma.certificate.dto.*;

import java.util.List;
import java.util.UUID;

@Component
public class CertificateMapper {

    public TemplateResponse toTemplateResponse(CertificateTemplate template) {
        return TemplateResponse.builder()
                .id(template.getId())
                .institutionId(template.getInstitutionId())
                .name(template.getName())
                .description(template.getDescription())
                .templateType(template.getTemplateType().name())
                .htmlContent(template.getHtmlContent())
                .cssContent(template.getCssContent())
                .logoUrl(template.getLogoUrl())
                .signatureLine1(template.getSignatureLine1())
                .signatureLine2(template.getSignatureLine2())
                .signatureLine3(template.getSignatureLine3())
                .isActive(template.getIsActive())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }

    public CertificateTemplate toTemplateEntity(CreateTemplateRequest request, UUID institutionId) {
        CertificateTemplate template = CertificateTemplate.builder()
                .name(request.getName())
                .description(request.getDescription())
                .templateType(request.getTemplateType())
                .htmlContent(request.getHtmlContent())
                .cssContent(request.getCssContent())
                .logoUrl(request.getLogoUrl())
                .signatureLine1(request.getSignatureLine1())
                .signatureLine2(request.getSignatureLine2())
                .signatureLine3(request.getSignatureLine3())
                .isActive(true)
                .build();
        template.setInstitutionId(institutionId);
        return template;
    }

    public CertificateResponse toCertificateResponse(Certificate certificate) {
        return CertificateResponse.builder()
                .id(certificate.getId())
                .institutionId(certificate.getInstitutionId())
                .templateId(certificate.getTemplateId())
                .studentId(certificate.getStudentId())
                .issuedBy(certificate.getIssuedBy())
                .serialNumber(certificate.getSerialNumber())
                .certificateType(certificate.getCertificateType().name())
                .title(certificate.getTitle())
                .courseTitle(certificate.getTitle())
                .description(certificate.getDescription())
                .studentName(certificate.getStudentName())
                .studentIdNumber(certificate.getStudentIdNumber())
                .courseOrProgramme(certificate.getCourseOrProgramme())
                .instructorName(certificate.getInstructorName())
                .grade(certificate.getGrade())
                .skills(certificate.getSkills())
                .completionDate(certificate.getCompletionDate())
                .issueDate(certificate.getIssueDate())
                .expiryDate(certificate.getExpiryDate())
                .status(certificate.getStatus().name())
                .verificationCode(certificate.getVerificationCode())
                .verificationUrl(certificate.getVerificationUrl())
                .qrCodeUrl(certificate.getQrCodeUrl())
                .revokedReason(certificate.getRevokedReason())
                .revokedAt(certificate.getRevokedAt())
                .metadata(certificate.getMetadata())
                .createdAt(certificate.getCreatedAt())
                .updatedAt(certificate.getUpdatedAt())
                .build();
    }

    public TranscriptResponse toTranscriptResponse(Transcript transcript, List<TranscriptEntry> entries) {
        List<TranscriptResponse.TranscriptEntryResponse> entryResponses = entries.stream()
                .map(this::toTranscriptEntryResponse)
                .toList();

        return TranscriptResponse.builder()
                .id(transcript.getId())
                .institutionId(transcript.getInstitutionId())
                .studentId(transcript.getStudentId())
                .issuedBy(transcript.getIssuedBy())
                .serialNumber(transcript.getSerialNumber())
                .academicYear(transcript.getAcademicYear())
                .term(transcript.getTerm())
                .status(transcript.getStatus().name())
                .totalSubjects(transcript.getTotalSubjects())
                .averageScore(transcript.getAverageScore())
                .classRank(transcript.getClassRank())
                .remarks(transcript.getRemarks())
                .generatedAt(transcript.getGeneratedAt())
                .issuedAt(transcript.getIssuedAt())
                .entries(entryResponses)
                .createdAt(transcript.getCreatedAt())
                .build();
    }

    public TranscriptResponse.TranscriptEntryResponse toTranscriptEntryResponse(TranscriptEntry entry) {
        return TranscriptResponse.TranscriptEntryResponse.builder()
                .id(entry.getId())
                .subjectName(entry.getSubjectName())
                .subjectCode(entry.getSubjectCode())
                .score(entry.getScore())
                .grade(entry.getGrade())
                .remarks(entry.getRemarks())
                .build();
    }
}
