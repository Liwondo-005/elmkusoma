package tz.elmkusoma.highereducation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.highereducation.dto.LearningModuleDTO;
import tz.elmkusoma.highereducation.service.LearningModuleService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/college/learner/modules")
@RequiredArgsConstructor
public class LearningModuleController {

    private final LearningModuleService learningModuleService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR')")
    public ResponseEntity<ApiResponse<LearningModuleDTO>> createModule(
            @RequestBody LearningModuleDTO dto,
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId) {
        // §48: server-authoritative scope — the client body never decides the tenant
        if (institutionId == null) {
            throw new ForbiddenException("Access denied");
        }
        dto.setInstitutionId(institutionId);
        return ResponseEntity.ok(ApiResponse.success("Module created", learningModuleService.createModule(dto)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR')")
    public ResponseEntity<ApiResponse<LearningModuleDTO>> getModule(
            @PathVariable UUID id,
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId,
            @RequestAttribute(value = "userRole", required = false) String role) {
        LearningModuleDTO dto = learningModuleService.getModule(id);
        if (!canAccessModule(dto, userId, institutionId, role)) {
            // §41/§60: object-level denial — indistinguishable from not-found (no resource oracle)
            throw new ResourceNotFoundException("Module not found");
        }
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<List<LearningModuleDTO>>> getStudentModules(
            @PathVariable UUID studentId,
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId,
            @RequestAttribute(value = "userRole", required = false) String role) {
        assertStudentListAccess(studentId, userId, role);
        return ResponseEntity.ok(ApiResponse.success(
                scopedToInstitution(learningModuleService.getStudentModules(studentId), institutionId, role)));
    }

    @GetMapping("/student/{studentId}/course/{courseId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<List<LearningModuleDTO>>> getStudentCourseModules(
            @PathVariable UUID studentId,
            @PathVariable UUID courseId,
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId,
            @RequestAttribute(value = "userRole", required = false) String role) {
        assertStudentListAccess(studentId, userId, role);
        return ResponseEntity.ok(ApiResponse.success(scopedToInstitution(
                learningModuleService.getStudentCourseModules(studentId, courseId), institutionId, role)));
    }

    @GetMapping("/institution/{institutionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR')")
    public ResponseEntity<ApiResponse<List<LearningModuleDTO>>> getInstitutionModules(
            @PathVariable UUID institutionId,
            @RequestAttribute(value = "institutionId", required = false) UUID callerInstitutionId,
            @RequestAttribute(value = "userRole", required = false) String role) {
        if (!"ADMIN".equals(role)
                && (callerInstitutionId == null || !callerInstitutionId.equals(institutionId))) {
            throw new ForbiddenException("Access denied");
        }
        return ResponseEntity.ok(ApiResponse.success(learningModuleService.getInstitutionModules(institutionId)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR')")
    public ResponseEntity<ApiResponse<LearningModuleDTO>> updateModule(
            @PathVariable UUID id,
            @RequestBody LearningModuleDTO dto,
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId,
            @RequestAttribute(value = "userRole", required = false) String role) {
        LearningModuleDTO existing = learningModuleService.getModule(id);
        assertCanManageModule(existing, institutionId, role);
        return ResponseEntity.ok(ApiResponse.success("Module updated", learningModuleService.updateModule(id, dto)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteModule(
            @PathVariable UUID id,
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId,
            @RequestAttribute(value = "userRole", required = false) String role) {
        LearningModuleDTO existing = learningModuleService.getModule(id);
        assertCanManageModule(existing, institutionId, role);
        learningModuleService.deleteModule(id);
        return ResponseEntity.ok(ApiResponse.success("Module deleted", null));
    }

    // ── §41/§60/§98 scope helpers ─────────────────────────────────

    /**
     * Fail-closed object-level access: learners own their records (ownership), staff need
     * institution scope; an unscoped platform ADMIN keeps its legacy platform-wide access.
     */
    private boolean canAccessModule(LearningModuleDTO dto, UUID userId, UUID institutionId, String role) {
        if (isLearnerRole(role)) {
            return userId != null && userId.equals(dto.getStudentId());
        }
        if (institutionId != null && institutionId.equals(dto.getInstitutionId())) {
            return true;
        }
        return institutionId == null && "ADMIN".equals(role);
    }

    /** Mutations are staff-only by @PreAuthorize and additionally require institution scope. */
    private void assertCanManageModule(LearningModuleDTO dto, UUID institutionId, String role) {
        if (institutionId != null && institutionId.equals(dto.getInstitutionId())) {
            return;
        }
        if (institutionId == null && "ADMIN".equals(role)) {
            return;
        }
        throw new ForbiddenException("Access denied");
    }

    /** §41: a learner may only list their own records. */
    private void assertStudentListAccess(UUID studentId, UUID userId, String role) {
        if (isLearnerRole(role) && (userId == null || !userId.equals(studentId))) {
            throw new ForbiddenException("You can only view your own modules");
        }
    }

    /** §35/§36: results always leave the server scoped — lists never cross tenant boundaries. */
    private List<LearningModuleDTO> scopedToInstitution(List<LearningModuleDTO> list, UUID institutionId, String role) {
        if (isLearnerRole(role)) {
            // ownership proven above; also drop any record injected from another institution
            return institutionId == null
                    ? list
                    : list.stream().filter(m -> institutionId.equals(m.getInstitutionId())).toList();
        }
        if (institutionId == null) {
            if ("ADMIN".equals(role)) {
                return list; // unscoped platform authority (legacy)
            }
            throw new ForbiddenException("Access denied");
        }
        return list.stream().filter(m -> institutionId.equals(m.getInstitutionId())).toList();
    }

    private boolean isLearnerRole(String role) {
        return "STUDENT".equals(role) || "OTHER_LEARNER".equals(role);
    }
}
