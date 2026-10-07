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
import tz.elmkusoma.highereducation.domain.DemonstrationStatus;
import tz.elmkusoma.highereducation.dto.DemonstrationDTO;
import tz.elmkusoma.highereducation.service.DemonstrationService;
import tz.elmkusoma.highereducation.service.HighEdIdentity;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/education/demonstrations")
@RequiredArgsConstructor
public class DemonstrationController {

    private final DemonstrationService demonstrationService;
    private final HighEdIdentity highEdIdentity;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR', 'STUDENT', 'OTHER_LEARNER')")
    public ResponseEntity<ApiResponse<List<DemonstrationDTO>>> listDemonstrations(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestParam(required = false) DemonstrationStatus status) {
        List<DemonstrationDTO> demonstrations = status != null
                ? demonstrationService.getDemonstrationsByStatus(institutionId, status)
                : demonstrationService.getDemonstrations(institutionId);
        return ResponseEntity.ok(ApiResponse.success(demonstrations));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR', 'STUDENT', 'OTHER_LEARNER')")
    public ResponseEntity<ApiResponse<DemonstrationDTO>> getDemonstration(
            @PathVariable UUID id,
            @RequestAttribute("userId") UUID callerUserId,
            @RequestAttribute(value = "userRole", required = false) String userRole,
            @RequestAttribute(value = "institutionId", required = false) UUID serverInstitutionId) {
        DemonstrationDTO demonstration = demonstrationService.getDemonstration(id);
        assertDemonstrationReadable(demonstration, callerUserId, userRole, serverInstitutionId, id);
        return ResponseEntity.ok(ApiResponse.success(demonstration));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<DemonstrationDTO>> createDemonstration(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @Valid @RequestBody DemonstrationDTO dto) {
        dto.setInstitutionId(institutionId);
        DemonstrationDTO created = demonstrationService.createDemonstration(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Demonstration created", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<DemonstrationDTO>> updateDemonstration(
            @PathVariable UUID id,
            @Valid @RequestBody DemonstrationDTO dto,
            @RequestAttribute("institutionId") UUID serverInstitutionId,
            @RequestAttribute("userRole") String userRole) {
        assertDemonstrationTenant(demonstrationService.getDemonstration(id), serverInstitutionId, userRole);
        DemonstrationDTO updated = demonstrationService.updateDemonstration(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Demonstration updated", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteDemonstration(
            @PathVariable UUID id,
            @RequestAttribute("institutionId") UUID serverInstitutionId,
            @RequestAttribute("userRole") String userRole) {
        assertDemonstrationTenant(demonstrationService.getDemonstration(id), serverInstitutionId, userRole);
        demonstrationService.deleteDemonstration(id);
        return ResponseEntity.ok(ApiResponse.success("Demonstration deleted", null));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<DemonstrationDTO>> submitForReview(
            @PathVariable UUID id,
            @RequestAttribute("userId") UUID callerUserId,
            @RequestAttribute("userRole") String userRole,
            @RequestAttribute("institutionId") UUID serverInstitutionId) {
        DemonstrationDTO existing = demonstrationService.getDemonstration(id);
        UUID learnerId = highEdIdentity.resolveStudentId(callerUserId, userRole, existing.getStudentId());
        highEdIdentity.assertStudentInInstitution(learnerId, serverInstitutionId);
        DemonstrationDTO submitted = demonstrationService.submitForReview(id);
        return ResponseEntity.ok(ApiResponse.success("Demonstration submitted for review", submitted));
    }

    @PutMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR')")
    public ResponseEntity<ApiResponse<DemonstrationDTO>> reviewDemonstration(
            @PathVariable UUID id,
            @RequestParam UUID reviewerId,
            @RequestParam DemonstrationStatus status,
            @RequestParam(required = false) String notes,
            @RequestParam(required = false) Integer score,
            @RequestAttribute("institutionId") UUID serverInstitutionId,
            @RequestAttribute("userRole") String userRole) {
        assertDemonstrationTenant(demonstrationService.getDemonstration(id), serverInstitutionId, userRole);
        DemonstrationDTO reviewed = demonstrationService.review(id, reviewerId, status, notes, score);
        return ResponseEntity.ok(ApiResponse.success("Demonstration reviewed", reviewed));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<List<DemonstrationDTO>>> getStudentDemonstrations(
            @PathVariable UUID studentId,
            @RequestAttribute("userId") UUID callerUserId,
            @RequestAttribute("userRole") String userRole,
            @RequestAttribute("institutionId") UUID serverInstitutionId) {
        UUID learnerId = highEdIdentity.resolveStudentId(callerUserId, userRole, studentId);
        highEdIdentity.assertStudentInInstitution(learnerId, serverInstitutionId);
        List<DemonstrationDTO> demonstrations = demonstrationService.getStudentDemonstrations(learnerId);
        return ResponseEntity.ok(ApiResponse.success(demonstrations));
    }

    private void assertDemonstrationReadable(DemonstrationDTO demonstration, UUID callerUserId, String userRole,
                                             UUID serverInstitutionId, UUID id) {
        if (HighEdIdentity.isLearner(userRole)) {
            if (demonstration.getStudentId() != null) {
                try {
                    highEdIdentity.resolveStudentId(callerUserId, userRole, demonstration.getStudentId());
                } catch (ForbiddenException e) {
                    throw new ResourceNotFoundException("Demonstration", "id", id);
                }
            } else if (serverInstitutionId == null || !serverInstitutionId.equals(demonstration.getInstitutionId())) {
                throw new ResourceNotFoundException("Demonstration", "id", id);
            }
            return;
        }
        assertDemonstrationTenant(demonstration, serverInstitutionId, userRole);
    }

    private void assertDemonstrationTenant(DemonstrationDTO demonstration, UUID serverInstitutionId, String userRole) {
        if ("ADMIN".equals(userRole)) {
            return;
        }
        if (serverInstitutionId == null || !serverInstitutionId.equals(demonstration.getInstitutionId())) {
            throw new ForbiddenException("Demonstration", "access");
        }
    }
}
