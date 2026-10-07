package tz.elmkusoma.highereducation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.highereducation.dto.ProfessionalDevelopmentGoalDTO;
import tz.elmkusoma.highereducation.service.HighEdIdentity;
import tz.elmkusoma.highereducation.service.ProfessionalDevelopmentGoalService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/college/learner/professional-dev")
@RequiredArgsConstructor
public class ProfessionalDevelopmentController {

    private final ProfessionalDevelopmentGoalService goalService;
    private final HighEdIdentity highEdIdentity;

    @PostMapping
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<ProfessionalDevelopmentGoalDTO>> createGoal(
            @RequestBody ProfessionalDevelopmentGoalDTO dto,
            @RequestAttribute("userId") UUID callerUserId,
            @RequestAttribute("userRole") String userRole,
            @RequestAttribute("institutionId") UUID serverInstitutionId) {
        UUID learnerId = highEdIdentity.resolveStudentId(callerUserId, userRole, dto.getStudentId());
        highEdIdentity.assertStudentInInstitution(learnerId, serverInstitutionId);
        dto.setStudentId(learnerId);
        dto.setInstitutionId(serverInstitutionId);
        return ResponseEntity.ok(ApiResponse.success("Goal created", goalService.createGoal(dto)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<ProfessionalDevelopmentGoalDTO>> getGoal(
            @PathVariable UUID id,
            @RequestAttribute("userId") UUID callerUserId,
            @RequestAttribute(value = "userRole", required = false) String userRole,
            @RequestAttribute(value = "institutionId", required = false) UUID serverInstitutionId) {
        ProfessionalDevelopmentGoalDTO goal = goalService.getGoal(id);
        assertGoalReadable(goal, callerUserId, userRole, serverInstitutionId, id);
        return ResponseEntity.ok(ApiResponse.success(goal));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<List<ProfessionalDevelopmentGoalDTO>>> getStudentGoals(
            @PathVariable UUID studentId,
            @RequestAttribute("userId") UUID callerUserId,
            @RequestAttribute("userRole") String userRole,
            @RequestAttribute("institutionId") UUID serverInstitutionId) {
        UUID learnerId = highEdIdentity.resolveStudentId(callerUserId, userRole, studentId);
        highEdIdentity.assertStudentInInstitution(learnerId, serverInstitutionId);
        return ResponseEntity.ok(ApiResponse.success(goalService.getStudentGoals(learnerId)));
    }

    @GetMapping("/student/{studentId}/status/{status}")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<List<ProfessionalDevelopmentGoalDTO>>> getGoalsByStatus(
            @PathVariable UUID studentId, @PathVariable String status,
            @RequestAttribute("userId") UUID callerUserId,
            @RequestAttribute("userRole") String userRole,
            @RequestAttribute("institutionId") UUID serverInstitutionId) {
        UUID learnerId = highEdIdentity.resolveStudentId(callerUserId, userRole, studentId);
        highEdIdentity.assertStudentInInstitution(learnerId, serverInstitutionId);
        return ResponseEntity.ok(ApiResponse.success(goalService.getStudentGoalsByStatus(learnerId, status)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<ProfessionalDevelopmentGoalDTO>> updateGoal(
            @PathVariable UUID id, @RequestBody ProfessionalDevelopmentGoalDTO dto,
            @RequestAttribute("userId") UUID callerUserId,
            @RequestAttribute("userRole") String userRole,
            @RequestAttribute("institutionId") UUID serverInstitutionId) {
        ProfessionalDevelopmentGoalDTO existing = goalService.getGoal(id);
        UUID learnerId = highEdIdentity.resolveStudentId(callerUserId, userRole, existing.getStudentId());
        highEdIdentity.assertStudentInInstitution(learnerId, serverInstitutionId);
        return ResponseEntity.ok(ApiResponse.success("Goal updated", goalService.updateGoal(id, dto)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteGoal(
            @PathVariable UUID id,
            @RequestAttribute("userId") UUID callerUserId,
            @RequestAttribute("userRole") String userRole,
            @RequestAttribute("institutionId") UUID serverInstitutionId) {
        ProfessionalDevelopmentGoalDTO existing = goalService.getGoal(id);
        UUID learnerId = highEdIdentity.resolveStudentId(callerUserId, userRole, existing.getStudentId());
        highEdIdentity.assertStudentInInstitution(learnerId, serverInstitutionId);
        goalService.deleteGoal(id);
        return ResponseEntity.ok(ApiResponse.success("Goal deleted", null));
    }

    private void assertGoalReadable(ProfessionalDevelopmentGoalDTO goal, UUID callerUserId, String userRole,
                                    UUID serverInstitutionId, UUID id) {
        if (HighEdIdentity.isLearner(userRole)) {
            if (goal.getStudentId() != null) {
                try {
                    highEdIdentity.resolveStudentId(callerUserId, userRole, goal.getStudentId());
                } catch (ForbiddenException e) {
                    throw new ResourceNotFoundException("Professional development goal", "id", id);
                }
            } else if (serverInstitutionId == null || !serverInstitutionId.equals(goal.getInstitutionId())) {
                throw new ResourceNotFoundException("Professional development goal", "id", id);
            }
            return;
        }
        highEdIdentity.assertStudentInInstitution(goal.getStudentId(), serverInstitutionId);
    }
}
