package tz.elmkusoma.nfe.material.controller;

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
import tz.elmkusoma.nfe.material.dto.MaterialRequest;
import tz.elmkusoma.nfe.material.dto.MaterialResponse;
import tz.elmkusoma.nfe.material.service.MaterialService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/nfe/materials")
@RequiredArgsConstructor
@Tag(name = "NFE Material Management", description = "CRUD operations for NFE learning materials")
public class MaterialController {

    private final MaterialService materialService;

    @PostMapping
    @Operation(summary = "Create a new learning material")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<MaterialResponse>> createMaterial(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @Valid @RequestBody MaterialRequest request) {
        MaterialResponse material = materialService.createMaterial(institutionId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Material created successfully", material));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a material by ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<MaterialResponse>> getMaterial(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID id) {
        MaterialResponse material = materialService.getMaterial(institutionId, id);
        return ResponseEntity.ok(ApiResponse.success(material));
    }

    @GetMapping
    @Operation(summary = "List all materials in an institution")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<PageResponse<MaterialResponse>>> listMaterials(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<MaterialResponse> materials = materialService.listMaterials(institutionId, page, size);
        return ResponseEntity.ok(ApiResponse.success(materials));
    }

    @GetMapping("/provider/{providerId}")
    @Operation(summary = "Get materials by provider ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<MaterialResponse>>> getMaterialsByProvider(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID providerId) {
        List<MaterialResponse> materials = materialService.getMaterialsByProvider(institutionId, providerId);
        return ResponseEntity.ok(ApiResponse.success(materials));
    }

    @GetMapping("/program/{programId}")
    @Operation(summary = "Get materials by program ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<MaterialResponse>>> getMaterialsByProgram(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID programId) {
        List<MaterialResponse> materials = materialService.getMaterialsByProgram(institutionId, programId);
        return ResponseEntity.ok(ApiResponse.success(materials));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a material")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<MaterialResponse>> updateMaterial(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID id,
            @Valid @RequestBody MaterialRequest request) {
        MaterialResponse material = materialService.updateMaterial(institutionId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Material updated successfully", material));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a material")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteMaterial(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID id) {
        materialService.deleteMaterial(institutionId, id);
        return ResponseEntity.ok(ApiResponse.success("Material deleted successfully", null));
    }
}
