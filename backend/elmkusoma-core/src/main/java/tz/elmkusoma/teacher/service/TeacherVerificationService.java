package tz.elmkusoma.teacher.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.administration.domain.VerificationRecord;
import tz.elmkusoma.administration.dto.VerificationSummaryResponse;
import tz.elmkusoma.administration.dto.VerificationSubmitRequest;
import tz.elmkusoma.administration.service.PlatformAdminService;
import tz.elmkusoma.teacher.domain.Teacher;
import tz.elmkusoma.teacher.dto.request.TeacherVerificationSubmitRequest;
import tz.elmkusoma.teacher.dto.response.TeacherVerificationStatusResponse;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Self-service teacher verification workflow. Submission reuses the existing generic
 * governance pipeline ({@code PlatformAdminService.submitVerification}, the
 * {@code verification_records} table and the Platform Admin review endpoints) — the same
 * flow institutions and providers already use. Submission is never self-approval: only a
 * Platform Admin can review, and review notifications are emitted for TEACHER records.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TeacherVerificationService {

    static final String VERIFICATION_TYPE = "TEACHING_SERVICE";
    /** notes is VARCHAR(255) since V81; keep submitted notes within the column limit. */
    static final int NOTES_MAX_LENGTH = 255;

    private static final ObjectMapper JSON = new ObjectMapper();

    private final TeacherVerificationStateService stateService;
    private final TeacherService teacherService;
    private final PlatformAdminService platformAdminService;

    @Transactional(readOnly = true)
    public TeacherVerificationStatusResponse getMyVerification(UUID userId, UUID institutionId) {
        Teacher teacher = teacherService.getOrCreateTeacherByUserId(userId, institutionId);
        String effectiveStatus = stateService.effectiveStatus(userId);

        List<TeacherVerificationStatusResponse.Record> records = stateService.history(userId).stream()
                .map(record -> TeacherVerificationStatusResponse.Record.builder()
                        .id(record.getId())
                        .status(record.getStatus())
                        .verificationType(record.getVerificationType())
                        .notes(record.getNotes())
                        .documents(record.getDocuments())
                        .metadata(record.getMetadata())
                        .submittedAt(record.getSubmittedAt())
                        .reviewedAt(record.getReviewedAt())
                        .reviewedBy(record.getReviewedBy())
                        .build())
                .toList();

        return TeacherVerificationStatusResponse.builder()
                .teacherId(teacher.getId())
                .userId(userId)
                .effectiveStatus(effectiveStatus)
                .formallyVerified(TeacherVerificationStateService.STATUS_APPROVED.equals(effectiveStatus))
                .requiredDocumentsByServiceType(TeacherVerificationStateService.REQUIRED_DOCUMENTS_BY_SERVICE_TYPE)
                .records(records)
                .build();
    }

    /**
     * Submit (or re-submit) a verification application. Preliminary automated checks run
     * here: service types must exist in the category matrix and every document required
     * for the declared categories must be attached. The record lands as PENDING for
     * Platform Admin review through the existing governance queue.
     */
    @Transactional
    public VerificationSummaryResponse submit(UUID userId, UUID institutionId,
                                              TeacherVerificationSubmitRequest request) {
        List<String> serviceTypes = TeacherVerificationStateService.normalizeServiceTypes(request.getServiceTypes());
        if (serviceTypes.isEmpty()) {
            throw new IllegalArgumentException("At least one service type is required");
        }
        List<String> requiredDocuments = stateService.requiredDocuments(serviceTypes);

        Set<String> providedTypes = new LinkedHashSet<>();
        for (TeacherVerificationSubmitRequest.DocumentSubmission document : request.getDocuments()) {
            if (document == null || document.getType() == null || document.getType().isBlank()) {
                throw new IllegalArgumentException("Every document needs a type");
            }
            if (document.getUrl() != null && !document.getUrl().isBlank()
                    && !document.getUrl().startsWith("http://") && !document.getUrl().startsWith("https://")) {
                throw new IllegalArgumentException("Document URLs must be http(s) links");
            }
            providedTypes.add(document.getType().trim().toUpperCase());
        }

        List<String> missing = requiredDocuments.stream()
                .filter(required -> !providedTypes.contains(required))
                .toList();
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Missing required documents " + missing
                    + " for service types " + serviceTypes);
        }

        VerificationSubmitRequest submission = new VerificationSubmitRequest();
        submission.setEntityType(TeacherVerificationStateService.ENTITY_TYPE);
        submission.setEntityId(userId);
        submission.setVerificationType(VERIFICATION_TYPE);
        submission.setSubmittedBy(userId);
        submission.setDocuments(toJson(request.getDocuments()));
        submission.setMetadata(toJson(Map.of("serviceTypes", serviceTypes)));
        submission.setNotes(truncate(request.getNotes()));

        // Ensure a teacher row exists so admins and the teacher share the same profile key.
        teacherService.getOrCreateTeacherByUserId(userId, institutionId);

        VerificationSummaryResponse response = platformAdminService.submitVerification(submission);
        log.info("Teacher verification submitted: user={} record={}", userId, response.getId());
        return response;
    }

    private String toJson(Object value) {
        try {
            return JSON.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize verification payload", e);
        }
    }

    static String truncate(String notes) {
        if (notes == null || notes.isBlank()) {
            return null;
        }
        String trimmed = notes.trim();
        return trimmed.length() <= NOTES_MAX_LENGTH ? trimmed : trimmed.substring(0, NOTES_MAX_LENGTH);
    }
}
