package tz.elmkusoma.highereducation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

    @GetMapping("/dashboard/{studentId}")
    public ResponseEntity<ApiResponse<HigherEducationDashboardDTO>> getDashboard(
            @PathVariable UUID studentId,
            @RequestHeader(value = "X-Institution-Id", defaultValue = "00000000-0000-0000-0000-000000000001") UUID institutionId,
            @RequestParam(defaultValue = "COLLEGE") String learningLevel) {
        HigherEducationDashboardDTO dashboard = dashboardService.getDashboard(studentId, institutionId, learningLevel);
        return ResponseEntity.ok(ApiResponse.success(dashboard));
    }

    @GetMapping("/enrollments/{studentId}")
    public ResponseEntity<ApiResponse<List<StudentCourseEnrollmentDTO>>> getEnrollments(@PathVariable UUID studentId) {
        return ResponseEntity.ok(ApiResponse.success(enrollmentService.getStudentEnrollments(studentId)));
    }

    @PostMapping("/enrollments")
    public ResponseEntity<ApiResponse<StudentCourseEnrollmentDTO>> createEnrollment(
            @RequestBody StudentCourseEnrollmentDTO dto,
            @RequestHeader(value = "X-Institution-Id", defaultValue = "00000000-0000-0000-0000-000000000001") UUID institutionId) {
        dto.setInstitutionId(institutionId);
        return ResponseEntity.ok(ApiResponse.success(enrollmentService.create(dto)));
    }

    @PutMapping("/enrollments/{id}")
    public ResponseEntity<ApiResponse<StudentCourseEnrollmentDTO>> updateEnrollment(
            @PathVariable UUID id, @RequestBody StudentCourseEnrollmentDTO dto) {
        return ResponseEntity.ok(ApiResponse.success(enrollmentService.update(id, dto)));
    }

    @GetMapping("/academic-record/{studentId}")
    public ResponseEntity<ApiResponse<AcademicRecordDTO>> getAcademicRecord(@PathVariable UUID studentId) {
        return ResponseEntity.ok(ApiResponse.success(academicRecordService.getLatestRecord(studentId)));
    }

    @GetMapping("/academic-record/{studentId}/history")
    public ResponseEntity<ApiResponse<List<AcademicRecordDTO>>> getAcademicRecordHistory(@PathVariable UUID studentId) {
        return ResponseEntity.ok(ApiResponse.success(academicRecordService.getStudentRecords(studentId)));
    }

    @PostMapping("/academic-record")
    public ResponseEntity<ApiResponse<AcademicRecordDTO>> createAcademicRecord(
            @RequestBody AcademicRecordDTO dto,
            @RequestHeader(value = "X-Institution-Id", defaultValue = "00000000-0000-0000-0000-000000000001") UUID institutionId) {
        dto.setInstitutionId(institutionId);
        return ResponseEntity.ok(ApiResponse.success(academicRecordService.create(dto)));
    }

    @PutMapping("/academic-record/{id}")
    public ResponseEntity<ApiResponse<AcademicRecordDTO>> updateAcademicRecord(
            @PathVariable UUID id, @RequestBody AcademicRecordDTO dto) {
        return ResponseEntity.ok(ApiResponse.success(academicRecordService.update(id, dto)));
    }

    @GetMapping("/career-profile/{studentId}")
    public ResponseEntity<ApiResponse<CareerProfileDTO>> getCareerProfile(@PathVariable UUID studentId) {
        return ResponseEntity.ok(ApiResponse.success(careerProfileService.getStudentProfile(studentId)));
    }

    @PostMapping("/career-profile/{studentId}")
    public ResponseEntity<ApiResponse<CareerProfileDTO>> upsertCareerProfile(
            @PathVariable UUID studentId,
            @RequestBody CareerProfileDTO dto,
            @RequestHeader(value = "X-Institution-Id", defaultValue = "00000000-0000-0000-0000-000000000001") UUID institutionId) {
        dto.setInstitutionId(institutionId);
        return ResponseEntity.ok(ApiResponse.success(careerProfileService.createOrUpdate(studentId, dto)));
    }
}
