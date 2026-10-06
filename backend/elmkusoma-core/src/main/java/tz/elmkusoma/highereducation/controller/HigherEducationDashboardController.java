package tz.elmkusoma.highereducation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.highereducation.dto.*;
import tz.elmkusoma.highereducation.service.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/education/higher-education")
@RequiredArgsConstructor
public class HigherEducationDashboardController {

    private final HigherEducationDashboardService dashboardService;
    private final StudentCourseEnrollmentService enrollmentService;
    private final AcademicRecordService academicRecordService;
    private final CareerProfileService careerProfileService;
    private final tz.elmkusoma.student.repository.StudentRepository studentRepository;
    private final tz.elmkusoma.parent.repository.ParentStudentLinkRepository parentStudentLinkRepository;

    @GetMapping("/dashboard/{studentId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER')")
    public ResponseEntity<ApiResponse<HigherEducationDashboardDTO>> getDashboard(
            @PathVariable UUID studentId,
            @RequestHeader(value = "X-Institution-Id", defaultValue = "00000000-0000-0000-0000-000000000001") UUID institutionId,
            @RequestParam(defaultValue = "COLLEGE") String learningLevel,
            @RequestAttribute("institutionId") UUID serverInstitutionId,
            @RequestAttribute("userId") UUID callerUserId,
            @RequestAttribute("userRole") String userRole) {
        UUID resolvedStudentId = verifyDashboardAccess(studentId, serverInstitutionId, callerUserId, userRole);
        HigherEducationDashboardDTO dashboard = dashboardService.getDashboard(resolvedStudentId, serverInstitutionId, learningLevel);
        return ResponseEntity.ok(ApiResponse.success(dashboard));
    }

    @GetMapping("/enrollments/{studentId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<List<StudentCourseEnrollmentDTO>>> getEnrollments(@PathVariable UUID studentId) {
        return ResponseEntity.ok(ApiResponse.success(enrollmentService.getStudentEnrollments(studentId)));
    }

    @PostMapping("/enrollments")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<StudentCourseEnrollmentDTO>> createEnrollment(
            @RequestBody StudentCourseEnrollmentDTO dto,
            @RequestHeader(value = "X-Institution-Id", defaultValue = "00000000-0000-0000-0000-000000000001") UUID institutionId) {
        dto.setInstitutionId(institutionId);
        return ResponseEntity.ok(ApiResponse.success(enrollmentService.create(dto)));
    }

    @PutMapping("/enrollments/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<StudentCourseEnrollmentDTO>> updateEnrollment(
            @PathVariable UUID id, @RequestBody StudentCourseEnrollmentDTO dto) {
        return ResponseEntity.ok(ApiResponse.success(enrollmentService.update(id, dto)));
    }

    @GetMapping("/academic-record/{studentId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<AcademicRecordDTO>> getAcademicRecord(@PathVariable UUID studentId) {
        return ResponseEntity.ok(ApiResponse.success(academicRecordService.getLatestRecord(studentId)));
    }

    @GetMapping("/academic-record/{studentId}/history")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<List<AcademicRecordDTO>>> getAcademicRecordHistory(@PathVariable UUID studentId) {
        return ResponseEntity.ok(ApiResponse.success(academicRecordService.getStudentRecords(studentId)));
    }

    @PostMapping("/academic-record")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<AcademicRecordDTO>> createAcademicRecord(
            @RequestBody AcademicRecordDTO dto,
            @RequestHeader(value = "X-Institution-Id", defaultValue = "00000000-0000-0000-0000-000000000001") UUID institutionId) {
        dto.setInstitutionId(institutionId);
        return ResponseEntity.ok(ApiResponse.success(academicRecordService.create(dto)));
    }

    @PutMapping("/academic-record/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<AcademicRecordDTO>> updateAcademicRecord(
            @PathVariable UUID id, @RequestBody AcademicRecordDTO dto) {
        return ResponseEntity.ok(ApiResponse.success(academicRecordService.update(id, dto)));
    }

    @GetMapping("/career-profile/{studentId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<CareerProfileDTO>> getCareerProfile(@PathVariable UUID studentId) {
        return ResponseEntity.ok(ApiResponse.success(careerProfileService.getStudentProfile(studentId)));
    }

    @PostMapping("/career-profile/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<CareerProfileDTO>> upsertCareerProfile(
            @PathVariable UUID studentId,
            @RequestBody CareerProfileDTO dto,
            @RequestHeader(value = "X-Institution-Id", defaultValue = "00000000-0000-0000-0000-000000000001") UUID institutionId) {
        dto.setInstitutionId(institutionId);
        return ResponseEntity.ok(ApiResponse.success(careerProfileService.createOrUpdate(studentId, dto)));
    }

    /**
     * Verifies the caller may read the dashboard for {@code requestedStudentId} and
     * returns the student row id to read.
     *
     * Clients identify the learner by their *user* id (the session user object has
     * no separate studentId), while the domain model keys students by the students
     * table id. Those are different values, so accepting only the row id made
     * /dashboard/learner fail with 403 ("You are not authorized to access this
     * Dashboard") and the page rendered "Failed to load dashboard data". A path
     * parameter equal to the caller's own user id is therefore resolved to that
     * caller's own student row. Ownership is unchanged: it only ever resolves to
     * the caller's own row, and any other value still has to match it.
     */
    private UUID verifyDashboardAccess(UUID studentId, UUID serverInstitutionId, UUID callerUserId, String userRole) {
        UUID effectiveStudentId = studentId;
        UUID ownStudentId = null;
        if (callerUserId != null) {
            ownStudentId = studentRepository.findByUserIdAndIsDeletedFalse(callerUserId)
                    .map(s -> s.getId())
                    .orElse(null);
        }
        if (ownStudentId != null && ownStudentId.equals(studentId)) {
            effectiveStudentId = ownStudentId;
        }
        final boolean requestedByOwnUserId = callerUserId != null && callerUserId.equals(studentId);
        final UUID targetStudentId = effectiveStudentId;

        if ("ADMIN".equals(userRole)) {
            return targetStudentId;
        }
        if ("STUDENT".equals(userRole) || "OTHER_LEARNER".equals(userRole)) {
            if (ownStudentId == null) {
                // No students row for this account (freshly registered or seeded
                // learner). Serve the caller's own dashboard rather than 403 -
                // it can only ever be their own id, and the dashboard renders
                // empty until student-scoped data exists.
                if (!requestedByOwnUserId) {
                    throw new tz.elmkusoma.exception.ForbiddenException("Dashboard", "access");
                }
            } else if (!ownStudentId.equals(targetStudentId)) {
                throw new tz.elmkusoma.exception.ForbiddenException("Dashboard", "access");
            }
        } else if ("PARENT".equals(userRole)) {
            boolean isChild = callerUserId != null && parentStudentLinkRepository.findAllByParentId(callerUserId).stream()
                    .anyMatch(link -> link.getStudentId().equals(targetStudentId));
            if (!isChild) {
                throw new tz.elmkusoma.exception.ForbiddenException("Dashboard", "access");
            }
        }
        studentRepository.findById(targetStudentId)
                .filter(s -> !Boolean.TRUE.equals(s.getIsDeleted()))
                .ifPresent(s -> {
                    if (s.getInstitutionId() == null || !s.getInstitutionId().equals(serverInstitutionId)) {
                        throw new tz.elmkusoma.exception.ForbiddenException("Dashboard", "access");
                    }
                });
        return targetStudentId;
    }
}
