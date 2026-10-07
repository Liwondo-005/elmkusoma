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
import tz.elmkusoma.highereducation.domain.LogbookEntry;
import tz.elmkusoma.highereducation.domain.PlacementStatus;
import tz.elmkusoma.highereducation.dto.FieldworkPlacementDTO;
import tz.elmkusoma.highereducation.dto.LogbookEntryDTO;
import tz.elmkusoma.highereducation.service.FieldworkService;
import tz.elmkusoma.highereducation.service.HighEdIdentity;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/education/fieldwork")
@RequiredArgsConstructor
public class FieldworkController {

    private final FieldworkService fieldworkService;
    private final tz.elmkusoma.highereducation.repository.LogbookEntryRepository logbookEntryRepository;
    private final HighEdIdentity highEdIdentity;

    // ── Placement Endpoints ──────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR', 'STUDENT', 'OTHER_LEARNER')")
    public ResponseEntity<ApiResponse<List<FieldworkPlacementDTO>>> listPlacements(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestParam(required = false) PlacementStatus status) {
        List<FieldworkPlacementDTO> placements = status != null
                ? fieldworkService.getPlacementsByStatus(institutionId, status)
                : fieldworkService.getPlacements(institutionId);
        return ResponseEntity.ok(ApiResponse.success(placements));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR', 'STUDENT', 'OTHER_LEARNER')")
    public ResponseEntity<ApiResponse<FieldworkPlacementDTO>> getPlacement(
            @PathVariable UUID id,
            @RequestAttribute("userId") UUID callerUserId,
            @RequestAttribute(value = "userRole", required = false) String userRole,
            @RequestAttribute(value = "institutionId", required = false) UUID serverInstitutionId) {
        FieldworkPlacementDTO placement = fieldworkService.getPlacement(id);
        assertPlacementReadable(placement, callerUserId, userRole, serverInstitutionId, id);
        return ResponseEntity.ok(ApiResponse.success(placement));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<FieldworkPlacementDTO>> createPlacement(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @Valid @RequestBody FieldworkPlacementDTO dto) {
        dto.setInstitutionId(institutionId);
        FieldworkPlacementDTO created = fieldworkService.createPlacement(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Placement created", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<FieldworkPlacementDTO>> updatePlacement(
            @PathVariable UUID id,
            @Valid @RequestBody FieldworkPlacementDTO dto,
            @RequestAttribute("institutionId") UUID serverInstitutionId,
            @RequestAttribute("userRole") String userRole) {
        assertPlacementTenant(fieldworkService.getPlacement(id), serverInstitutionId, userRole);
        FieldworkPlacementDTO updated = fieldworkService.updatePlacement(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Placement updated", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deletePlacement(
            @PathVariable UUID id,
            @RequestAttribute("institutionId") UUID serverInstitutionId,
            @RequestAttribute("userRole") String userRole) {
        assertPlacementTenant(fieldworkService.getPlacement(id), serverInstitutionId, userRole);
        fieldworkService.deletePlacement(id);
        return ResponseEntity.ok(ApiResponse.success("Placement deleted", null));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<List<FieldworkPlacementDTO>>> getStudentPlacements(
            @PathVariable UUID studentId,
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userId") UUID callerUserId,
            @RequestAttribute("userRole") String userRole,
            @RequestAttribute("institutionId") UUID serverInstitutionId) {
        UUID learnerId = highEdIdentity.resolveStudentId(callerUserId, userRole, studentId);
        highEdIdentity.assertStudentInInstitution(learnerId, serverInstitutionId);
        List<FieldworkPlacementDTO> placements = fieldworkService.getStudentPlacements(learnerId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(placements));
    }

    @GetMapping("/instructor/{instructorId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR')")
    public ResponseEntity<ApiResponse<List<FieldworkPlacementDTO>>> getInstructorPlacements(
            @PathVariable UUID instructorId) {
        List<FieldworkPlacementDTO> placements = fieldworkService.getPlacementsByInstructor(instructorId);
        return ResponseEntity.ok(ApiResponse.success(placements));
    }

    @PutMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR')")
    public ResponseEntity<ApiResponse<FieldworkPlacementDTO>> completePlacement(
            @PathVariable UUID id,
            @RequestAttribute("institutionId") UUID serverInstitutionId,
            @RequestAttribute("userRole") String userRole) {
        assertPlacementTenant(fieldworkService.getPlacement(id), serverInstitutionId, userRole);
        FieldworkPlacementDTO completed = fieldworkService.completePlacement(id);
        return ResponseEntity.ok(ApiResponse.success("Placement completed", completed));
    }

    // ── Logbook Endpoints ────────────────────────────────────────

    @PostMapping("/{id}/logbook")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR', 'STUDENT', 'OTHER_LEARNER')")
    public ResponseEntity<ApiResponse<LogbookEntryDTO>> addLogbookEntry(
            @PathVariable UUID id,
            @Valid @RequestBody LogbookEntryDTO dto,
            @RequestAttribute("userId") UUID callerUserId,
            @RequestAttribute("userRole") String userRole,
            @RequestAttribute("institutionId") UUID serverInstitutionId) {
        FieldworkPlacementDTO placement = fieldworkService.getPlacement(id);
        UUID learnerId = highEdIdentity.resolveStudentId(callerUserId, userRole, placement.getStudentId());
        highEdIdentity.assertStudentInInstitution(learnerId, serverInstitutionId);
        LogbookEntryDTO created = fieldworkService.addLogbookEntry(id, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Logbook entry added", created));
    }

    @GetMapping("/{id}/logbook")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR', 'STUDENT', 'OTHER_LEARNER')")
    public ResponseEntity<ApiResponse<List<LogbookEntryDTO>>> getLogbookEntries(
            @PathVariable UUID id,
            @RequestAttribute("userId") UUID callerUserId,
            @RequestAttribute(value = "userRole", required = false) String userRole,
            @RequestAttribute(value = "institutionId", required = false) UUID serverInstitutionId) {
        assertPlacementReadable(fieldworkService.getPlacement(id), callerUserId, userRole, serverInstitutionId, id);
        List<LogbookEntryDTO> entries = fieldworkService.getLogbookEntries(id);
        return ResponseEntity.ok(ApiResponse.success(entries));
    }

    @GetMapping("/{id}/logbook/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR')")
    public ResponseEntity<ApiResponse<List<LogbookEntryDTO>>> getPendingLogbookEntries(
            @PathVariable UUID id,
            @RequestAttribute("userId") UUID callerUserId,
            @RequestAttribute(value = "userRole", required = false) String userRole,
            @RequestAttribute(value = "institutionId", required = false) UUID serverInstitutionId) {
        assertPlacementReadable(fieldworkService.getPlacement(id), callerUserId, userRole, serverInstitutionId, id);
        List<LogbookEntryDTO> entries = fieldworkService.getPendingLogbookEntries(id);
        return ResponseEntity.ok(ApiResponse.success(entries));
    }

    @PutMapping("/logbook/{entryId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR', 'STUDENT', 'OTHER_LEARNER')")
    public ResponseEntity<ApiResponse<LogbookEntryDTO>> updateLogbookEntry(
            @PathVariable UUID entryId,
            @Valid @RequestBody LogbookEntryDTO dto,
            @RequestAttribute("userId") UUID callerUserId,
            @RequestAttribute("userRole") String userRole,
            @RequestAttribute("institutionId") UUID serverInstitutionId) {
        LogbookEntry entry = getLogbookEntry(entryId);
        FieldworkPlacementDTO placement = fieldworkService.getPlacement(entry.getPlacementId());
        UUID learnerId = highEdIdentity.resolveStudentId(callerUserId, userRole, placement.getStudentId());
        highEdIdentity.assertStudentInInstitution(learnerId, serverInstitutionId);
        LogbookEntryDTO updated = fieldworkService.updateLogbookEntry(entryId, dto);
        return ResponseEntity.ok(ApiResponse.success("Logbook entry updated", updated));
    }

    @PutMapping("/logbook/{entryId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR')")
    public ResponseEntity<ApiResponse<LogbookEntryDTO>> approveLogbookEntry(
            @PathVariable UUID entryId,
            @RequestAttribute(value = "userId", required = false) UUID approvedBy,
            @RequestAttribute("institutionId") UUID serverInstitutionId,
            @RequestAttribute("userRole") String userRole) {
        LogbookEntry entry = getLogbookEntry(entryId);
        assertPlacementTenant(fieldworkService.getPlacement(entry.getPlacementId()), serverInstitutionId, userRole);
        LogbookEntryDTO approved = fieldworkService.approveLogbookEntry(entryId, approvedBy);
        return ResponseEntity.ok(ApiResponse.success("Logbook entry approved", approved));
    }

    private LogbookEntry getLogbookEntry(UUID entryId) {
        return logbookEntryRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("LogbookEntry", "id", entryId));
    }

    private void assertPlacementReadable(FieldworkPlacementDTO placement, UUID callerUserId, String userRole,
                                         UUID serverInstitutionId, UUID id) {
        if (HighEdIdentity.isLearner(userRole)) {
            if (placement.getStudentId() != null) {
                try {
                    highEdIdentity.resolveStudentId(callerUserId, userRole, placement.getStudentId());
                } catch (ForbiddenException e) {
                    throw new ResourceNotFoundException("Placement", "id", id);
                }
            } else if (serverInstitutionId == null || !serverInstitutionId.equals(placement.getInstitutionId())) {
                throw new ResourceNotFoundException("Placement", "id", id);
            }
            return;
        }
        assertPlacementTenant(placement, serverInstitutionId, userRole);
    }

    private void assertPlacementTenant(FieldworkPlacementDTO placement, UUID serverInstitutionId, String userRole) {
        if ("ADMIN".equals(userRole)) {
            return;
        }
        if (serverInstitutionId == null || !serverInstitutionId.equals(placement.getInstitutionId())) {
            throw new ForbiddenException("Placement", "access");
        }
    }
}
