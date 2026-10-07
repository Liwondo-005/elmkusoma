package tz.elmkusoma.highereducation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.exception.ForbiddenException;
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
    private final HighEdIdentity highEdIdentity;

    @GetMapping("/dashboard/{studentId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER')")
    public ResponseEntity<ApiResponse<HigherEducationDashboardDTO>> getDashboard(
            @PathVariable UUID studentId,
            @RequestParam(required = false) String learningLevel,
            @RequestAttribute("institutionId") UUID serverInstitutionId,
            @RequestAttribute("userId") UUID callerUserId,
            @RequestAttribute("userRole") String userRole) {
        UUID learnerId = highEdIdentity.resolveStudentId(callerUserId, userRole, studentId);
        highEdIdentity.assertStudentInInstitution(learnerId, serverInstitutionId);
        String contextLevel = highEdIdentity.resolveLearningLevel(callerUserId, learnerId, learningLevel);
        HigherEducationDashboardDTO dashboard =
                dashboardService.getDashboard(learnerId, serverInstitutionId, contextLevel);
        return ResponseEntity.ok(ApiResponse.success(dashboard));
    }

    @GetMapping("/enrollments/{studentId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<List<StudentCourseEnrollmentDTO>>> getEnrollments(
            @PathVariable UUID studentId,
            @RequestAttribute("institutionId") UUID serverInstitutionId,
            @RequestAttribute("userId") UUID callerUserId,
            @RequestAttribute("userRole") String userRole) {
        UUID learnerId = highEdIdentity.resolveStudentId(callerUserId, userRole, studentId);
        highEdIdentity.assertStudentInInstitution(learnerId, serverInstitutionId);
        return ResponseEntity.ok(ApiResponse.success(enrollmentService.getStudentEnrollments(learnerId)));
    }

    @PostMapping("/enrollments")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<StudentCourseEnrollmentDTO>> createEnrollment(
            @RequestBody StudentCourseEnrollmentDTO dto,
            @RequestAttribute(value = "institutionId", required = false) UUID serverInstitutionId,
            @RequestHeader(value = "X-Institution-Id", required = false) UUID headerInstitutionId) {
        UUID institutionId = effectiveInstitution(serverInstitutionId, headerInstitutionId);
        if (dto.getStudentId() != null) {
            highEdIdentity.assertStudentInInstitution(dto.getStudentId(), institutionId);
        }
        dto.setInstitutionId(institutionId);
        return ResponseEntity.ok(ApiResponse.success(enrollmentService.create(dto)));
    }

    @PutMapping("/enrollments/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<StudentCourseEnrollmentDTO>> updateEnrollment(
            @PathVariable UUID id,
            @RequestBody StudentCourseEnrollmentDTO dto,
            @RequestAttribute(value = "institutionId", required = false) UUID serverInstitutionId,
            @RequestHeader(value = "X-Institution-Id", required = false) UUID headerInstitutionId) {
        UUID institutionId = effectiveInstitution(serverInstitutionId, headerInstitutionId);
        return ResponseEntity.ok(ApiResponse.success(enrollmentService.update(id, dto, institutionId)));
    }

    @GetMapping("/academic-record/{studentId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<AcademicRecordDTO>> getAcademicRecord(
            @PathVariable UUID studentId,
            @RequestAttribute("institutionId") UUID serverInstitutionId,
            @RequestAttribute("userId") UUID callerUserId,
            @RequestAttribute("userRole") String userRole) {
        UUID learnerId = highEdIdentity.resolveStudentId(callerUserId, userRole, studentId);
        highEdIdentity.assertStudentInInstitution(learnerId, serverInstitutionId);
        return ResponseEntity.ok(ApiResponse.success(academicRecordService.getLatestRecord(learnerId)));
    }

    @GetMapping("/academic-record/{studentId}/history")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<List<AcademicRecordDTO>>> getAcademicRecordHistory(
            @PathVariable UUID studentId,
            @RequestAttribute("institutionId") UUID serverInstitutionId,
            @RequestAttribute("userId") UUID callerUserId,
            @RequestAttribute("userRole") String userRole) {
        UUID learnerId = highEdIdentity.resolveStudentId(callerUserId, userRole, studentId);
        highEdIdentity.assertStudentInInstitution(learnerId, serverInstitutionId);
        return ResponseEntity.ok(ApiResponse.success(academicRecordService.getStudentRecords(learnerId)));
    }

    @PostMapping("/academic-record")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<AcademicRecordDTO>> createAcademicRecord(
            @RequestBody AcademicRecordDTO dto,
            @RequestAttribute(value = "institutionId", required = false) UUID serverInstitutionId,
            @RequestHeader(value = "X-Institution-Id", required = false) UUID headerInstitutionId) {
        UUID institutionId = effectiveInstitution(serverInstitutionId, headerInstitutionId);
        if (dto.getStudentId() != null) {
            highEdIdentity.assertStudentInInstitution(dto.getStudentId(), institutionId);
        }
        dto.setInstitutionId(institutionId);
        return ResponseEntity.ok(ApiResponse.success(academicRecordService.create(dto)));
    }

    @PutMapping("/academic-record/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<AcademicRecordDTO>> updateAcademicRecord(
            @PathVariable UUID id,
            @RequestBody AcademicRecordDTO dto,
            @RequestAttribute(value = "institutionId", required = false) UUID serverInstitutionId,
            @RequestHeader(value = "X-Institution-Id", required = false) UUID headerInstitutionId) {
        UUID institutionId = effectiveInstitution(serverInstitutionId, headerInstitutionId);
        return ResponseEntity.ok(ApiResponse.success(academicRecordService.update(id, dto, institutionId)));
    }

    @GetMapping("/career-profile/{studentId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<CareerProfileDTO>> getCareerProfile(
            @PathVariable UUID studentId,
            @RequestAttribute("institutionId") UUID serverInstitutionId,
            @RequestAttribute("userId") UUID callerUserId,
            @RequestAttribute("userRole") String userRole) {
        UUID learnerId = highEdIdentity.resolveStudentId(callerUserId, userRole, studentId);
        highEdIdentity.assertStudentInInstitution(learnerId, serverInstitutionId);
        return ResponseEntity.ok(ApiResponse.success(careerProfileService.getStudentProfile(learnerId)));
    }

    @PostMapping("/career-profile/{studentId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<CareerProfileDTO>> upsertCareerProfile(
            @PathVariable UUID studentId,
            @RequestBody CareerProfileDTO dto,
            @RequestAttribute(value = "institutionId", required = false) UUID serverInstitutionId,
            @RequestHeader(value = "X-Institution-Id", required = false) UUID headerInstitutionId,
            @RequestAttribute("userId") UUID callerUserId,
            @RequestAttribute("userRole") String userRole) {
        UUID learnerId = highEdIdentity.resolveStudentId(callerUserId, userRole, studentId);
        highEdIdentity.assertStudentInInstitution(learnerId, serverInstitutionId);
        dto.setInstitutionId(effectiveInstitution(serverInstitutionId, headerInstitutionId));
        return ResponseEntity.ok(ApiResponse.success(careerProfileService.createOrUpdate(learnerId, dto)));
    }

    private UUID effectiveInstitution(UUID serverInstitutionId, UUID headerInstitutionId) {
        if (serverInstitutionId != null) {
            return serverInstitutionId;
        }
        if (headerInstitutionId != null) {
            return headerInstitutionId;
        }
        throw new ForbiddenException("Access denied");
    }
}
