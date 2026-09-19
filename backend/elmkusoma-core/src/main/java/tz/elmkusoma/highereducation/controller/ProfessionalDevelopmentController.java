package tz.elmkusoma.highereducation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.highereducation.dto.ProfessionalDevelopmentGoalDTO;
import tz.elmkusoma.highereducation.service.ProfessionalDevelopmentGoalService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/college/learner/professional-dev")
@RequiredArgsConstructor
public class ProfessionalDevelopmentController {

    private final ProfessionalDevelopmentGoalService goalService;

    @PostMapping
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<ProfessionalDevelopmentGoalDTO>> createGoal(
            @RequestBody ProfessionalDevelopmentGoalDTO dto) {
        return ResponseEntity.ok(ApiResponse.success("Goal created", goalService.createGoal(dto)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<ProfessionalDevelopmentGoalDTO>> getGoal(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(goalService.getGoal(id)));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<List<ProfessionalDevelopmentGoalDTO>>> getStudentGoals(
            @PathVariable UUID studentId) {
        return ResponseEntity.ok(ApiResponse.success(goalService.getStudentGoals(studentId)));
    }

    @GetMapping("/student/{studentId}/status/{status}")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<List<ProfessionalDevelopmentGoalDTO>>> getGoalsByStatus(
            @PathVariable UUID studentId, @PathVariable String status) {
        return ResponseEntity.ok(ApiResponse.success(goalService.getStudentGoalsByStatus(studentId, status)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<ProfessionalDevelopmentGoalDTO>> updateGoal(
            @PathVariable UUID id, @RequestBody ProfessionalDevelopmentGoalDTO dto) {
        return ResponseEntity.ok(ApiResponse.success("Goal updated", goalService.updateGoal(id, dto)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteGoal(@PathVariable UUID id) {
        goalService.deleteGoal(id);
        return ResponseEntity.ok(ApiResponse.success("Goal deleted", null));
    }
}
