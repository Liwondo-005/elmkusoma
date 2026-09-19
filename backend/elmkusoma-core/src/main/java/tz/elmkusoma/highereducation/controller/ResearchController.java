package tz.elmkusoma.highereducation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.highereducation.domain.ResearchStatus;
import tz.elmkusoma.highereducation.dto.ResearchMilestoneDTO;
import tz.elmkusoma.highereducation.dto.ResearchProjectDTO;
import tz.elmkusoma.highereducation.dto.ResearchResourceDTO;
import tz.elmkusoma.highereducation.service.ResearchService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/education/research")
@RequiredArgsConstructor
public class ResearchController {

    private final ResearchService researchService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR', 'STUDENT', 'OTHER_LEARNER')")
    public ResponseEntity<ApiResponse<List<ResearchProjectDTO>>> listResearchProjects(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestParam(required = false) ResearchStatus status) {
        List<ResearchProjectDTO> projects = status != null
                ? researchService.getResearchProjectsByStatus(institutionId, status)
                : researchService.getResearchProjects(institutionId);
        return ResponseEntity.ok(ApiResponse.success(projects));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR', 'STUDENT', 'OTHER_LEARNER')")
    public ResponseEntity<ApiResponse<ResearchProjectDTO>> getResearchProject(@PathVariable UUID id) {
        ResearchProjectDTO project = researchService.getResearchProject(id);
        return ResponseEntity.ok(ApiResponse.success(project));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<ResearchProjectDTO>> createResearchProject(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @Valid @RequestBody ResearchProjectDTO dto) {
        dto.setInstitutionId(institutionId);
        ResearchProjectDTO created = researchService.createResearchProject(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Research project created", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<ResearchProjectDTO>> updateResearchProject(
            @PathVariable UUID id,
            @Valid @RequestBody ResearchProjectDTO dto) {
        ResearchProjectDTO updated = researchService.updateResearchProject(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Research project updated", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteResearchProject(@PathVariable UUID id) {
        researchService.deleteResearchProject(id);
        return ResponseEntity.ok(ApiResponse.success("Research project deleted", null));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<List<ResearchProjectDTO>>> getStudentResearchProjects(
            @PathVariable UUID studentId,
            @RequestHeader("X-Institution-Id") UUID institutionId) {
        List<ResearchProjectDTO> projects = researchService.getStudentResearchProjects(studentId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(projects));
    }

    @GetMapping("/supervisor/{supervisorId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR')")
    public ResponseEntity<ApiResponse<List<ResearchProjectDTO>>> getSupervisorResearchProjects(
            @PathVariable UUID supervisorId) {
        List<ResearchProjectDTO> projects = researchService.getResearchProjectsBySupervisor(supervisorId);
        return ResponseEntity.ok(ApiResponse.success(projects));
    }

    // ── Milestones ───────────────────────────────────────────────

    @PostMapping("/{id}/milestones")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<ResearchMilestoneDTO>> addMilestone(
            @PathVariable UUID id,
            @Valid @RequestBody ResearchMilestoneDTO dto) {
        ResearchMilestoneDTO created = researchService.addMilestone(id, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Milestone added", created));
    }

    @GetMapping("/{id}/milestones")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR', 'STUDENT', 'OTHER_LEARNER')")
    public ResponseEntity<ApiResponse<List<ResearchMilestoneDTO>>> getMilestones(@PathVariable UUID id) {
        List<ResearchMilestoneDTO> milestones = researchService.getMilestones(id);
        return ResponseEntity.ok(ApiResponse.success(milestones));
    }

    @PutMapping("/milestones/{milestoneId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<ResearchMilestoneDTO>> updateMilestone(
            @PathVariable UUID milestoneId,
            @Valid @RequestBody ResearchMilestoneDTO dto) {
        ResearchMilestoneDTO updated = researchService.updateMilestone(milestoneId, dto);
        return ResponseEntity.ok(ApiResponse.success("Milestone updated", updated));
    }

    @PutMapping("/milestones/{milestoneId}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> completeMilestone(@PathVariable UUID milestoneId) {
        researchService.completeMilestone(milestoneId);
        return ResponseEntity.ok(ApiResponse.success("Milestone completed", null));
    }

    // ── Resources ────────────────────────────────────────────────

    @PostMapping("/{id}/resources")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<ResearchResourceDTO>> addResource(
            @PathVariable UUID id,
            @Valid @RequestBody ResearchResourceDTO dto) {
        ResearchResourceDTO created = researchService.addResource(id, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Resource added", created));
    }

    @GetMapping("/{id}/resources")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR', 'STUDENT', 'OTHER_LEARNER')")
    public ResponseEntity<ApiResponse<List<ResearchResourceDTO>>> getResources(@PathVariable UUID id) {
        List<ResearchResourceDTO> resources = researchService.getResources(id);
        return ResponseEntity.ok(ApiResponse.success(resources));
    }

    @PutMapping("/resources/{resourceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<ResearchResourceDTO>> updateResource(
            @PathVariable UUID resourceId,
            @Valid @RequestBody ResearchResourceDTO dto) {
        ResearchResourceDTO updated = researchService.updateResource(resourceId, dto);
        return ResponseEntity.ok(ApiResponse.success("Resource updated", updated));
    }

    @DeleteMapping("/resources/{resourceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteResource(@PathVariable UUID resourceId) {
        researchService.deleteResource(resourceId);
        return ResponseEntity.ok(ApiResponse.success("Resource deleted", null));
    }
}
