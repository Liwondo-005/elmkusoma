package tz.elmkusoma.certificate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.certificate.domain.Certificate;
import tz.elmkusoma.certificate.domain.Certificate.CertificateStatus;
import tz.elmkusoma.certificate.domain.Certificate.CertificateType;
import tz.elmkusoma.certificate.domain.CertificateTemplate;
import tz.elmkusoma.certificate.domain.Transcript;
import tz.elmkusoma.certificate.domain.TranscriptEntry;
import tz.elmkusoma.certificate.dto.*;
import tz.elmkusoma.certificate.mapper.CertificateMapper;
import tz.elmkusoma.certificate.repository.*;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.shared.domain.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CertificateService {

    private final CertificateTemplateRepository templateRepository;
    private final CertificateRepository certificateRepository;
    private final TranscriptRepository transcriptRepository;
    private final TranscriptEntryRepository transcriptEntryRepository;
    private final CertificateMapper certificateMapper;

    // ── Template Management ──

    public TemplateResponse createTemplate(CreateTemplateRequest request, UUID institutionId) {
        if (templateRepository.existsByNameAndInstitutionIdAndIsDeletedFalse(request.getName(), institutionId)) {
            throw new IllegalArgumentException("Template with name '" + request.getName() + "' already exists");
        }

        CertificateTemplate template = certificateMapper.toTemplateEntity(request, institutionId);
        templateRepository.save(template);

        log.info("Created certificate template: {} for institution: {}", template.getName(), institutionId);
        return certificateMapper.toTemplateResponse(template);
    }

    @Transactional(readOnly = true)
    public List<TemplateResponse> getTemplates(UUID institutionId) {
        return templateRepository.findAllByInstitutionId(institutionId).stream()
                .map(certificateMapper::toTemplateResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TemplateResponse getTemplateById(UUID templateId, UUID institutionId) {
        CertificateTemplate template = templateRepository.findByIdAndIsDeletedFalse(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("CertificateTemplate", "id", templateId));

        if (!template.getInstitutionId().equals(institutionId)) {
            throw new ForbiddenException("template", "access");
        }

        return certificateMapper.toTemplateResponse(template);
    }

    // ── Certificate Generation ──

    public CertificateResponse generateCertificate(GenerateCertificateRequest request, UUID institutionId, UUID issuedBy) {
        // Validate template exists
        CertificateTemplate template = templateRepository.findByIdAndIsDeletedFalse(request.getTemplateId())
                .orElseThrow(() -> new ResourceNotFoundException("CertificateTemplate", "id", request.getTemplateId()));

        if (!template.getInstitutionId().equals(institutionId)) {
            throw new ForbiddenException("template", "use");
        }

        // Generate serial number
        String serialNumber = generateSerialNumber(institutionId, request.getCertificateType());

        // Generate verification code
        String verificationCode = generateVerificationCode();

        // Build verification URL
        String verificationUrl = "/api/v1/certificates/" + verificationCode + "/verify";

        Certificate certificate = Certificate.builder()
                .templateId(request.getTemplateId())
                .studentId(request.getStudentId())
                .issuedBy(issuedBy)
                .serialNumber(serialNumber)
                .certificateType(request.getCertificateType())
                .title(request.getTitle())
                .description(request.getDescription())
                .studentName(request.getStudentName())
                .studentIdNumber(request.getStudentIdNumber())
                .courseOrProgramme(request.getCourseOrProgramme())
                .instructorName(request.getInstructorName())
                .grade(request.getGrade())
                .skills(request.getSkills())
                .completionDate(request.getCompletionDate())
                .issueDate(LocalDateTime.now())
                .expiryDate(request.getExpiryDate())
                .status(CertificateStatus.DRAFT)
                .verificationCode(verificationCode)
                .verificationUrl(verificationUrl)
                .build();
        certificate.setInstitutionId(institutionId);

        certificateRepository.save(certificate);

        log.info("Generated certificate: {} for student: {} in institution: {}",
                serialNumber, request.getStudentId(), institutionId);
        return certificateMapper.toCertificateResponse(certificate);
    }

    public CertificateResponse issueCertificate(UUID certificateId, UUID institutionId) {
        Certificate certificate = certificateRepository.findByIdAndIsDeletedFalse(certificateId)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate", "id", certificateId));

        if (!certificate.getInstitutionId().equals(institutionId)) {
            throw new ForbiddenException("certificate", "issue");
        }

        if (certificate.getStatus() != CertificateStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT certificates can be issued");
        }

        certificate.setStatus(CertificateStatus.ISSUED);
        certificate.setIssueDate(LocalDateTime.now());
        certificateRepository.save(certificate);

        log.info("Issued certificate: {}", certificate.getSerialNumber());
        return certificateMapper.toCertificateResponse(certificate);
    }

    public CertificateResponse revokeCertificate(UUID certificateId, UUID institutionId, RevokeCertificateRequest request) {
        Certificate certificate = certificateRepository.findByIdAndIsDeletedFalse(certificateId)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate", "id", certificateId));

        if (!certificate.getInstitutionId().equals(institutionId)) {
            throw new ForbiddenException("certificate", "revoke");
        }

        if (certificate.getStatus() == CertificateStatus.REVOKED) {
            throw new IllegalStateException("Certificate is already revoked");
        }

        certificate.setStatus(CertificateStatus.REVOKED);
        certificate.setRevokedReason(request.getReason());
        certificate.setRevokedAt(LocalDateTime.now());
        certificateRepository.save(certificate);

        log.info("Revoked certificate: {} Reason: {}", certificate.getSerialNumber(), request.getReason());
        return certificateMapper.toCertificateResponse(certificate);
    }

    // ── Certificate Verification ──

    @Transactional(readOnly = true)
    public CertificateVerificationResponse verifyCertificate(String verificationCode) {
        Certificate certificate = certificateRepository.findByVerificationCodeAndIsDeletedFalse(verificationCode)
                .orElse(null);

        if (certificate == null) {
            return CertificateVerificationResponse.builder()
                    .valid(false)
                    .message("Certificate not found")
                    .build();
        }

        if (certificate.getStatus() == CertificateStatus.REVOKED) {
            return CertificateVerificationResponse.builder()
                    .valid(false)
                    .id(certificate.getId().toString())
                    .serialNumber(certificate.getSerialNumber())
                    .studentName(certificate.getStudentName())
                    .certificateType(certificate.getCertificateType().name())
                    .title(certificate.getTitle())
                    .courseTitle(certificate.getTitle())
                    .instructorName(certificate.getInstructorName())
                    .grade(certificate.getGrade())
                    .skills(certificate.getSkills())
                    .completionDate(certificate.getCompletionDate())
                    .status(certificate.getStatus().name())
                    .message("This certificate has been revoked: " + certificate.getRevokedReason())
                    .build();
        }

        if (certificate.getExpiryDate() != null && certificate.getExpiryDate().isBefore(LocalDate.now())) {
            return CertificateVerificationResponse.builder()
                    .valid(false)
                    .id(certificate.getId().toString())
                    .serialNumber(certificate.getSerialNumber())
                    .studentName(certificate.getStudentName())
                    .certificateType(certificate.getCertificateType().name())
                    .title(certificate.getTitle())
                    .courseTitle(certificate.getTitle())
                    .instructorName(certificate.getInstructorName())
                    .grade(certificate.getGrade())
                    .skills(certificate.getSkills())
                    .completionDate(certificate.getCompletionDate())
                    .status(certificate.getStatus().name())
                    .message("This certificate has expired")
                    .build();
        }

        return CertificateVerificationResponse.builder()
                .valid(certificate.getStatus() == CertificateStatus.ISSUED)
                .id(certificate.getId().toString())
                .serialNumber(certificate.getSerialNumber())
                .studentName(certificate.getStudentName())
                .certificateType(certificate.getCertificateType().name())
                .title(certificate.getTitle())
                .courseTitle(certificate.getTitle())
                .instructorName(certificate.getInstructorName())
                .grade(certificate.getGrade())
                .skills(certificate.getSkills())
                .completionDate(certificate.getCompletionDate())
                .status(certificate.getStatus().name())
                .issueDate(certificate.getIssueDate())
                .message("Certificate is valid and verified")
                .build();
    }

    // ── Certificate Queries ──

    @Transactional(readOnly = true)
    public List<CertificateResponse> getCertificatesByStudent(UUID studentId) {
        return certificateRepository.findAllByStudentId(studentId).stream()
                .map(certificateMapper::toCertificateResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CertificateResponse> getCertificatesByInstitution(UUID institutionId) {
        return certificateRepository.findAllByInstitutionId(institutionId).stream()
                .map(certificateMapper::toCertificateResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CertificateResponse getCertificateById(UUID certificateId, UUID institutionId) {
        Certificate certificate = certificateRepository.findByIdAndIsDeletedFalse(certificateId)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate", "id", certificateId));

        if (!certificate.getInstitutionId().equals(institutionId)) {
            throw new ForbiddenException("certificate", "access");
        }

        return certificateMapper.toCertificateResponse(certificate);
    }

    // ── Transcript Generation ──

    public TranscriptResponse generateTranscript(GenerateTranscriptRequest request, UUID institutionId, UUID issuedBy) {
        // Check for existing transcript for this period
        if (request.getAcademicYear() != null && request.getTerm() != null) {
            transcriptRepository.findByStudentAndPeriod(request.getStudentId(), request.getAcademicYear(), request.getTerm())
                    .ifPresent(t -> {
                        throw new IllegalArgumentException("Transcript already exists for this student and period");
                    });
        }

        String serialNumber = generateTranscriptSerialNumber(institutionId);

        Transcript transcript = Transcript.builder()
                .studentId(request.getStudentId())
                .issuedBy(issuedBy)
                .serialNumber(serialNumber)
                .academicYear(request.getAcademicYear())
                .term(request.getTerm())
                .status(Transcript.TranscriptStatus.DRAFT)
                .generatedAt(LocalDateTime.now())
                .remarks(request.getRemarks())
                .build();
        transcript.setInstitutionId(institutionId);

        transcriptRepository.save(transcript);

        // Save entries
        if (request.getEntries() != null && !request.getEntries().isEmpty()) {
            for (GenerateTranscriptRequest.TranscriptEntryRequest entryReq : request.getEntries()) {
                TranscriptEntry entry = TranscriptEntry.builder()
                        .transcriptId(transcript.getId())
                        .subjectName(entryReq.getSubjectName())
                        .subjectCode(entryReq.getSubjectCode())
                        .score(entryReq.getScore())
                        .grade(entryReq.getGrade())
                        .remarks(entryReq.getRemarks())
                        .build();
                entry.setInstitutionId(institutionId);
                transcriptEntryRepository.save(entry);
            }

            transcript.setTotalSubjects(request.getEntries().size());
            transcriptRepository.save(transcript);
        }

        log.info("Generated transcript: {} for student: {} in institution: {}",
                serialNumber, request.getStudentId(), institutionId);

        List<TranscriptEntry> entries = transcriptEntryRepository.findAllByTranscriptId(transcript.getId());
        return certificateMapper.toTranscriptResponse(transcript, entries);
    }

    public TranscriptResponse issueTranscript(UUID transcriptId, UUID institutionId) {
        Transcript transcript = transcriptRepository.findByIdAndIsDeletedFalse(transcriptId)
                .orElseThrow(() -> new ResourceNotFoundException("Transcript", "id", transcriptId));

        if (!transcript.getInstitutionId().equals(institutionId)) {
            throw new ForbiddenException("transcript", "issue");
        }

        if (transcript.getStatus() != Transcript.TranscriptStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT transcripts can be issued");
        }

        transcript.setStatus(Transcript.TranscriptStatus.ISSUED);
        transcript.setIssuedAt(LocalDateTime.now());
        transcriptRepository.save(transcript);

        List<TranscriptEntry> entries = transcriptEntryRepository.findAllByTranscriptId(transcriptId);
        return certificateMapper.toTranscriptResponse(transcript, entries);
    }

    @Transactional(readOnly = true)
    public List<TranscriptResponse> getTranscriptsByStudent(UUID studentId) {
        List<Transcript> transcripts = transcriptRepository.findAllByStudentId(studentId);
        return transcripts.stream()
                .map(t -> {
                    List<TranscriptEntry> entries = transcriptEntryRepository.findAllByTranscriptId(t.getId());
                    return certificateMapper.toTranscriptResponse(t, entries);
                })
                .toList();
    }

    // ── Helpers ──

    private String generateSerialNumber(UUID institutionId, CertificateType type) {
        String prefix = switch (type) {
            case COMPLETION -> "CERT-CMP";
            case ACHIEVEMENT -> "CERT-ACH";
            case PARTICIPATION -> "CERT-PAR";
            case TRANSCRIPT -> "CERT-TRN";
        };
        long count = certificateRepository.countByInstitutionId(institutionId) + 1;
        int year = LocalDate.now().getYear();
        return String.format("%s-%d-%06d", prefix, year, count);
    }

    private String generateTranscriptSerialNumber(UUID institutionId) {
        long count = transcriptRepository.findAllByInstitutionId(institutionId).size() + 1;
        int year = LocalDate.now().getYear();
        return String.format("TRAN-%d-%06d", year, count);
    }

    private String generateVerificationCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}
