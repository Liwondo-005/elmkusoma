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
import tz.elmkusoma.highereducation.domain.ResearchMilestone;
import tz.elmkusoma.highereducation.domain.ResearchResource;
import tz.elmkusoma.highereducation.domain.ResearchStatus;
import tz.elmkusoma.highereducation.dto.ResearchMilestoneDTO;
import tz.elmkusoma.highereducation.dto.ResearchProjectDTO;
import tz.elmkusoma.highereducation.dto.ResearchResourceDTO;
import tz.elmkusoma.highereducation.repository.ResearchMilestoneRepository;
import tz.elmkusoma.highereducation.repository.ResearchResourceRepository;
import tz.elmkusoma.highereducation.service.HighEdIdentity;
import tz.elmkusoma.highereducation.service.ResearchService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/education/research")
@RequiredArgsConstructor
public class ResearchController {

    private final ResearchService researchService;
    private final ResearchMilestoneRepository researchMilestoneRepository;
    private final ResearchResourceRepository researchResourceRepository;
    private final HighEdIdentity highEdIdentity;

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
    public ResponseEntity<ApiResponse<ResearchProjectDTO>> getResearchProject(
            @PathVariable UUID id,
            @RequestAttribute("userId") UUID callerUserId,
            @RequestAttribute(value = "userRole", required = false) String userRole,
            @RequestAttribute(value = "institutionId", required = false) UUID serverInstitutionId) {
        ResearchProjectDTO project = researchService.getResearchProject(id);
        assertResearchReadable(project, callerUserId, userRole, serverInstitutionId, id);
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
            @Valid @RequestBody ResearchProjectDTO dto,
            @RequestAttribute("institutionId") UUID serverInstitutionId,
            @RequestAttribute("userRole") String userRole) {
        assertResearchTenant(researchService.getResearchProject(id), serverInstitutionId, userRole);
        ResearchProjectDTO updated = researchService.updateResearchProject(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Research project updated", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteResearchProject(
            @PathVariable UUID id,
            @RequestAttribute("institutionId") UUID serverInstitutionId,
            @RequestAttribute("userRole") String userRole) {
        assertResearchTenant(researchService.getResearchProject(id), serverInstitutionId, userRole);
        researchService.deleteResearchProject(id);
        return ResponseEntity.ok(ApiResponse.success("Research project deleted", null));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<List<ResearchProjectDTO>>> getStudentResearchProjects(
            @PathVariable UUID studentId,
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userId") UUID callerUserId,
            @RequestAttribute("userRole") String userRole,
            @RequestAttribute("institutionId") UUID serverInstitutionId) {
        UUID learnerId = highEdIdentity.resolveStudentId(callerUserId, userRole, studentId);
        highEdIdentity.assertStudentInInstitution(learnerId, serverInstitutionId);
        List<ResearchProjectDTO> projects = researchService.getStudentResearchProjects(learnerId, institutionId);
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
    public ResponseEntity<ApiResponse<List<ResearchMilestoneDTO>>> getMilestones(
            @PathVariable UUID id,
            @RequestAttribute("userId") UUID callerUserId,
            @RequestAttribute(value = "userRole", required = false) String userRole,
            @RequestAttribute(value = "institutionId", required = false) UUID serverInstitutionId) {
        assertResearchReadable(researchService.getResearchProject(id), callerUserId, userRole, serverInstitutionId, id);
        List<ResearchMilestoneDTO> milestones = researchService.getMilestones(id);
        return ResponseEntity.ok(ApiResponse.success(milestones));
    }

    @PutMapping("/milestones/{milestoneId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<ResearchMilestoneDTO>> updateMilestone(
            @PathVariable UUID milestoneId,
            @Valid @RequestBody ResearchMilestoneDTO dto,
            @RequestAttribute(value = "institutionId", required = false) UUID serverInstitutionId,
            @RequestAttribute(value = "userRole", required = false) String userRole) {
        ResearchMilestone milestone = researchMilestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException("ResearchMilestone", "id", milestoneId));
        assertChildTenant(milestone.getInstitutionId(), serverInstitutionId, userRole, "Research milestone");
        ResearchMilestoneDTO updated = researchService.updateMilestone(milestoneId, dto);
        return ResponseEntity.ok(ApiResponse.success("Milestone updated", updated));
    }

    @PutMapping("/milestones/{milestoneId}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> completeMilestone(
            @PathVariable UUID milestoneId,
            @RequestAttribute(value = "institutionId", required = false) UUID serverInstitutionId,
            @RequestAttribute(value = "userRole", required = false) String userRole) {
        ResearchMilestone milestone = researchMilestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException("ResearchMilestone", "id", milestoneId));
        assertChildTenant(milestone.getInstitutionId(), serverInstitutionId, userRole, "Research milestone");
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
    public ResponseEntity<ApiResponse<List<ResearchResourceDTO>>> getResources(
            @PathVariable UUID id,
            @RequestAttribute("userId") UUID callerUserId,
            @RequestAttribute(value = "userRole", required = false) String userRole,
            @RequestAttribute(value = "institutionId", required = false) UUID serverInstitutionId) {
        assertResearchReadable(researchService.getResearchProject(id), callerUserId, userRole, serverInstitutionId, id);
        List<ResearchResourceDTO> resources = researchService.getResources(id);
        return ResponseEntity.ok(ApiResponse.success(resources));
    }

    @PutMapping("/resources/{resourceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<ResearchResourceDTO>> updateResource(
            @PathVariable UUID resourceId,
            @Valid @RequestBody ResearchResourceDTO dto,
            @RequestAttribute(value = "institutionId", required = false) UUID serverInstitutionId,
            @RequestAttribute(value = "userRole", required = false) String userRole) {
        ResearchResource resource = researchResourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("ResearchResource", "id", resourceId));
        assertChildTenant(resource.getInstitutionId(), serverInstitutionId, userRole, "Research resource");
        ResearchResourceDTO updated = researchService.updateResource(resourceId, dto);
        return ResponseEntity.ok(ApiResponse.success("Resource updated", updated));
    }

    @DeleteMapping("/resources/{resourceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteResource(
            @PathVariable UUID resourceId,
            @RequestAttribute(value = "institutionId", required = false) UUID serverInstitutionId,
            @RequestAttribute(value = "userRole", required = false) String userRole) {
        ResearchResource resource = researchResourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("ResearchResource", "id", resourceId));
        assertChildTenant(resource.getInstitutionId(), serverInstitutionId, userRole, "Research resource");
        researchService.deleteResource(resourceId);
        return ResponseEntity.ok(ApiResponse.success("Resource deleted", null));
    }

    private void assertResearchReadable(ResearchProjectDTO project, UUID callerUserId, String userRole,
                                        UUID serverInstitutionId, UUID id) {
        if (HighEdIdentity.isLearner(userRole)) {
            if (project.getStudentId() != null) {
                try {
                    highEdIdentity.resolveStudentId(callerUserId, userRole, project.getStudentId());
                } catch (ForbiddenException e) {
                    throw new ResourceNotFoundException("Research project", "id", id);
                }
            } else if (serverInstitutionId == null || !serverInstitutionId.equals(project.getInstitutionId())) {
                throw new ResourceNotFoundException("Research project", "id", id);
            }
            return;
        }
        assertResearchTenant(project, serverInstitutionId, userRole);
    }

    private void assertChildTenant(UUID targetInstitutionId, UUID serverInstitutionId,
                                   String userRole, String resourceName) {
        if (targetInstitutionId == null || serverInstitutionId == null || "ADMIN".equals(userRole)) {
            return;
        }
        if (!serverInstitutionId.equals(targetInstitutionId)) {
            throw new ForbiddenException(resourceName, "access");
        }
    }

    private void assertResearchTenant(ResearchProjectDTO project, UUID serverInstitutionId, String userRole) {
        if ("ADMIN".equals(userRole)) {
            return;
        }
        if (serverInstitutionId == null || !serverInstitutionId.equals(project.getInstitutionId())) {
            throw new ForbiddenException("Research project", "access");
        }
    }
}
