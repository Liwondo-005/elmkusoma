package tz.elmkusoma.highereducation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.highereducation.domain.PlacementStatus;
import tz.elmkusoma.highereducation.dto.FieldworkPlacementDTO;
import tz.elmkusoma.highereducation.dto.LogbookEntryDTO;
import tz.elmkusoma.highereducation.service.FieldworkService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/education/fieldwork")
@RequiredArgsConstructor
public class FieldworkController {

    private final FieldworkService fieldworkService;

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
    public ResponseEntity<ApiResponse<FieldworkPlacementDTO>> getPlacement(@PathVariable UUID id) {
        FieldworkPlacementDTO placement = fieldworkService.getPlacement(id);
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
            @Valid @RequestBody FieldworkPlacementDTO dto) {
        FieldworkPlacementDTO updated = fieldworkService.updatePlacement(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Placement updated", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deletePlacement(@PathVariable UUID id) {
        fieldworkService.deletePlacement(id);
        return ResponseEntity.ok(ApiResponse.success("Placement deleted", null));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<List<FieldworkPlacementDTO>>> getStudentPlacements(
            @PathVariable UUID studentId,
            @RequestHeader("X-Institution-Id") UUID institutionId) {
        List<FieldworkPlacementDTO> placements = fieldworkService.getStudentPlacements(studentId, institutionId);
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
    public ResponseEntity<ApiResponse<FieldworkPlacementDTO>> completePlacement(@PathVariable UUID id) {
        FieldworkPlacementDTO completed = fieldworkService.completePlacement(id);
        return ResponseEntity.ok(ApiResponse.success("Placement completed", completed));
    }

    // ── Logbook Endpoints ────────────────────────────────────────

    @PostMapping("/{id}/logbook")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR', 'STUDENT', 'OTHER_LEARNER')")
    public ResponseEntity<ApiResponse<LogbookEntryDTO>> addLogbookEntry(
            @PathVariable UUID id,
            @Valid @RequestBody LogbookEntryDTO dto) {
        LogbookEntryDTO created = fieldworkService.addLogbookEntry(id, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Logbook entry added", created));
    }

    @GetMapping("/{id}/logbook")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR', 'STUDENT', 'OTHER_LEARNER')")
    public ResponseEntity<ApiResponse<List<LogbookEntryDTO>>> getLogbookEntries(
            @PathVariable UUID id) {
        List<LogbookEntryDTO> entries = fieldworkService.getLogbookEntries(id);
        return ResponseEntity.ok(ApiResponse.success(entries));
    }

    @GetMapping("/{id}/logbook/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR')")
    public ResponseEntity<ApiResponse<List<LogbookEntryDTO>>> getPendingLogbookEntries(
            @PathVariable UUID id) {
        List<LogbookEntryDTO> entries = fieldworkService.getPendingLogbookEntries(id);
        return ResponseEntity.ok(ApiResponse.success(entries));
    }

    @PutMapping("/logbook/{entryId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR', 'STUDENT', 'OTHER_LEARNER')")
    public ResponseEntity<ApiResponse<LogbookEntryDTO>> updateLogbookEntry(
            @PathVariable UUID entryId,
            @Valid @RequestBody LogbookEntryDTO dto) {
        LogbookEntryDTO updated = fieldworkService.updateLogbookEntry(entryId, dto);
        return ResponseEntity.ok(ApiResponse.success("Logbook entry updated", updated));
    }

    @PutMapping("/logbook/{entryId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR')")
    public ResponseEntity<ApiResponse<LogbookEntryDTO>> approveLogbookEntry(
            @PathVariable UUID entryId,
            @RequestAttribute(value = "userId", required = false) UUID approvedBy) {
        LogbookEntryDTO approved = fieldworkService.approveLogbookEntry(entryId, approvedBy);
        return ResponseEntity.ok(ApiResponse.success("Logbook entry approved", approved));
    }
}
