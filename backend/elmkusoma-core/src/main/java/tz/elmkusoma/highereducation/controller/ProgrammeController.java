package tz.elmkusoma.highereducation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.academic.domain.EducationLevel;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.highereducation.domain.ProgrammeType;
import tz.elmkusoma.highereducation.dto.ProgrammeDTO;
import tz.elmkusoma.highereducation.service.ProgrammeService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/education/programmes")
@RequiredArgsConstructor
@Tag(name = "Programme Management", description = "CRUD operations for higher education programmes")
public class ProgrammeController {

    private final ProgrammeService programmeService;

    @GetMapping
    @Operation(summary = "List all programmes in an institution")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<List<ProgrammeDTO>>> listProgrammes(
            @RequestHeader("X-Institution-Id") UUID institutionId) {
        List<ProgrammeDTO> programmes = programmeService.listByInstitution(institutionId);
        return ResponseEntity.ok(ApiResponse.success(programmes));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a programme by ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<ProgrammeDTO>> getProgramme(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID id) {
        ProgrammeDTO programme = programmeService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(programme));
    }

    @GetMapping("/by-level/{educationLevel}")
    @Operation(summary = "List programmes by education level")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<List<ProgrammeDTO>>> listByEducationLevel(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable EducationLevel educationLevel) {
        List<ProgrammeDTO> programmes = programmeService.listByEducationLevel(institutionId, educationLevel);
        return ResponseEntity.ok(ApiResponse.success(programmes));
    }

    @GetMapping("/by-type/{programmeType}")
    @Operation(summary = "List programmes by programme type")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<List<ProgrammeDTO>>> listByProgrammeType(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable ProgrammeType programmeType) {
        List<ProgrammeDTO> programmes = programmeService.listByProgrammeType(institutionId, programmeType);
        return ResponseEntity.ok(ApiResponse.success(programmes));
    }

    @GetMapping("/count")
    @Operation(summary = "Count programmes in an institution")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<Long>> countProgrammes(
            @RequestHeader("X-Institution-Id") UUID institutionId) {
        long count = programmeService.countByInstitution(institutionId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @PostMapping
    @Operation(summary = "Create a new programme")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<ProgrammeDTO>> createProgramme(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @Valid @RequestBody ProgrammeDTO request) {
        ProgrammeDTO programme = programmeService.create(institutionId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Programme created successfully", programme));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a programme")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<ProgrammeDTO>> updateProgramme(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID id,
            @Valid @RequestBody ProgrammeDTO request) {
        ProgrammeDTO programme = programmeService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("Programme updated successfully", programme));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a programme")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProgramme(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID id) {
        programmeService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Programme deleted successfully", null));
    }
}
