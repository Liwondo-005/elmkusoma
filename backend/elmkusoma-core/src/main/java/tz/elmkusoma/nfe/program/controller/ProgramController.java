package tz.elmkusoma.nfe.program.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.common.PageResponse;
import tz.elmkusoma.nfe.program.dto.ProgramRequest;
import tz.elmkusoma.nfe.program.dto.ProgramResponse;
import tz.elmkusoma.nfe.program.service.ProgramService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/nfe/programs")
@RequiredArgsConstructor
@Tag(name = "NFE Program Management", description = "CRUD operations for NFE programs")
public class ProgramController {

    private final ProgramService programService;

    @PostMapping
    @Operation(summary = "Create a new NFE program")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<ProgramResponse>> createProgram(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @Valid @RequestBody ProgramRequest request) {
        ProgramResponse program = programService.createProgram(institutionId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Program created successfully", program));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an NFE program by ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<ProgramResponse>> getProgram(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID id) {
        ProgramResponse program = programService.getProgram(institutionId, id);
        return ResponseEntity.ok(ApiResponse.success(program));
    }

    @GetMapping
    @Operation(summary = "List all NFE programs in an institution")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<PageResponse<ProgramResponse>>> listPrograms(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<ProgramResponse> programs = programService.listPrograms(institutionId, page, size);
        return ResponseEntity.ok(ApiResponse.success(programs));
    }

    @GetMapping("/provider/{providerId}")
    @Operation(summary = "Get programs by provider")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<ProgramResponse>>> getProgramsByProvider(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID providerId) {
        List<ProgramResponse> programs = programService.getProgramsByProvider(institutionId, providerId);
        return ResponseEntity.ok(ApiResponse.success(programs));
    }

    @GetMapping("/published")
    @Operation(summary = "Get all published NFE programs")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<ProgramResponse>>> getPublishedPrograms(
            @RequestHeader("X-Institution-Id") UUID institutionId) {
        List<ProgramResponse> programs = programService.getPublishedPrograms(institutionId);
        return ResponseEntity.ok(ApiResponse.success(programs));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an NFE program")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<ProgramResponse>> updateProgram(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID id,
            @Valid @RequestBody ProgramRequest request) {
        ProgramResponse program = programService.updateProgram(institutionId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Program updated successfully", program));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete an NFE program")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProgram(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID id) {
        programService.deleteProgram(institutionId, id);
        return ResponseEntity.ok(ApiResponse.success("Program deleted successfully", null));
    }
}
