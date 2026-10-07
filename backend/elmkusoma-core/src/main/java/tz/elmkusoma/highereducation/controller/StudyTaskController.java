package tz.elmkusoma.highereducation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.highereducation.dto.StudyTaskDTO;
import tz.elmkusoma.highereducation.service.HighEdIdentity;
import tz.elmkusoma.highereducation.service.StudyTaskService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/education/study-tasks")
@RequiredArgsConstructor
public class StudyTaskController {

    private final StudyTaskService studyTaskService;
    private final HighEdIdentity highEdIdentity;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR', 'STUDENT', 'OTHER_LEARNER')")
    public ResponseEntity<ApiResponse<List<StudyTaskDTO>>> listStudyTasks(
            @RequestHeader("X-Institution-Id") UUID institutionId) {
        List<StudyTaskDTO> tasks = studyTaskService.getStudyTasks(institutionId);
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR', 'STUDENT', 'OTHER_LEARNER')")
    public ResponseEntity<ApiResponse<StudyTaskDTO>> getStudyTask(
            @PathVariable UUID id,
            @RequestAttribute("userId") UUID callerUserId,
            @RequestAttribute(value = "userRole", required = false) String userRole,
            @RequestAttribute(value = "institutionId", required = false) UUID serverInstitutionId) {
        StudyTaskDTO task = studyTaskService.getStudyTask(id);
        assertTaskReadable(task, callerUserId, userRole, serverInstitutionId, id);
        return ResponseEntity.ok(ApiResponse.success(task));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<StudyTaskDTO>> createStudyTask(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @Valid @RequestBody StudyTaskDTO dto) {
        dto.setInstitutionId(institutionId);
        StudyTaskDTO created = studyTaskService.createStudyTask(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Study task created", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<StudyTaskDTO>> updateStudyTask(
            @PathVariable UUID id,
            @Valid @RequestBody StudyTaskDTO dto,
            @RequestAttribute("institutionId") UUID serverInstitutionId,
            @RequestAttribute("userRole") String userRole) {
        assertStudyTaskTenant(studyTaskService.getStudyTask(id), serverInstitutionId, userRole);
        StudyTaskDTO updated = studyTaskService.updateStudyTask(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Study task updated", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteStudyTask(
            @PathVariable UUID id,
            @RequestAttribute("institutionId") UUID serverInstitutionId,
            @RequestAttribute("userRole") String userRole) {
        assertStudyTaskTenant(studyTaskService.getStudyTask(id), serverInstitutionId, userRole);
        studyTaskService.deleteStudyTask(id);
        return ResponseEntity.ok(ApiResponse.success("Study task deleted", null));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<List<StudyTaskDTO>>> getStudentTasks(
            @PathVariable UUID studentId,
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userId") UUID callerUserId,
            @RequestAttribute("userRole") String userRole,
            @RequestAttribute("institutionId") UUID serverInstitutionId) {
        UUID learnerId = highEdIdentity.resolveStudentId(callerUserId, userRole, studentId);
        highEdIdentity.assertStudentInInstitution(learnerId, serverInstitutionId);
        List<StudyTaskDTO> tasks = studyTaskService.getStudentTasks(learnerId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    @GetMapping("/student/{studentId}/today")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<List<StudyTaskDTO>>> getTodayTasks(
            @PathVariable UUID studentId,
            @RequestAttribute("userId") UUID callerUserId,
            @RequestAttribute("userRole") String userRole,
            @RequestAttribute("institutionId") UUID serverInstitutionId) {
        UUID learnerId = highEdIdentity.resolveStudentId(callerUserId, userRole, studentId);
        highEdIdentity.assertStudentInInstitution(learnerId, serverInstitutionId);
        List<StudyTaskDTO> tasks = studyTaskService.getTodayTasks(learnerId);
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    @GetMapping("/student/{studentId}/week")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<List<StudyTaskDTO>>> getWeekTasks(
            @PathVariable UUID studentId,
            @RequestAttribute("userId") UUID callerUserId,
            @RequestAttribute("userRole") String userRole,
            @RequestAttribute("institutionId") UUID serverInstitutionId) {
        UUID learnerId = highEdIdentity.resolveStudentId(callerUserId, userRole, studentId);
        highEdIdentity.assertStudentInInstitution(learnerId, serverInstitutionId);
        List<StudyTaskDTO> tasks = studyTaskService.getWeekTasks(learnerId);
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    @PutMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<StudyTaskDTO>> completeStudyTask(
            @PathVariable UUID id,
            @RequestAttribute("userId") UUID callerUserId,
            @RequestAttribute("userRole") String userRole,
            @RequestAttribute("institutionId") UUID serverInstitutionId) {
        StudyTaskDTO existing = studyTaskService.getStudyTask(id);
        UUID learnerId = highEdIdentity.resolveStudentId(callerUserId, userRole, existing.getStudentId());
        highEdIdentity.assertStudentInInstitution(learnerId, serverInstitutionId);
        StudyTaskDTO completed = studyTaskService.completeStudyTask(id);
        return ResponseEntity.ok(ApiResponse.success("Study task completed", completed));
    }

    private void assertTaskReadable(StudyTaskDTO task, UUID callerUserId, String userRole,
                                    UUID serverInstitutionId, UUID id) {
        if (HighEdIdentity.isLearner(userRole)) {
            if (task.getStudentId() != null) {
                try {
                    highEdIdentity.resolveStudentId(callerUserId, userRole, task.getStudentId());
                } catch (ForbiddenException e) {
                    throw new ResourceNotFoundException("Study task", "id", id);
                }
            } else if (serverInstitutionId == null || !serverInstitutionId.equals(task.getInstitutionId())) {
                throw new ResourceNotFoundException("Study task", "id", id);
            }
            return;
        }
        assertStudyTaskTenant(task, serverInstitutionId, userRole);
    }

    private void assertStudyTaskTenant(StudyTaskDTO task, UUID serverInstitutionId, String userRole) {
        if ("ADMIN".equals(userRole)) {
            return;
        }
        if (serverInstitutionId == null || !serverInstitutionId.equals(task.getInstitutionId())) {
            throw new ForbiddenException("Study task", "access");
        }
    }
}
