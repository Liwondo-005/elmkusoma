package tz.elmkusoma.learning.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.learning.domain.ResourceAnalytics;
import tz.elmkusoma.learning.dto.ResourceAnnotationRequest;
import tz.elmkusoma.learning.dto.ResourceAnnotationResponse;
import tz.elmkusoma.learning.dto.ResourceAnalyticsSummary;
import tz.elmkusoma.learning.dto.ResourceRequest;
import tz.elmkusoma.learning.dto.ResourceResponse;
import tz.elmkusoma.learning.service.ResourceAnnotationService;
import tz.elmkusoma.learning.service.ResourceAnalyticsService;
import tz.elmkusoma.learning.service.ResourceService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/resources")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'STUDENT')")
@Tag(name = "Resource Management", description = "Unified resource management for learning content")
public class ResourceController {

    private final ResourceService resourceService;
    private final ResourceAnnotationService annotationService;
    private final ResourceAnalyticsService analyticsService;

    @PostMapping
    @Operation(summary = "Create a new resource")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<ResourceResponse>> createResource(
            @Valid @RequestBody ResourceRequest request,
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestAttribute("userId") UUID userId) {
        ResourceResponse response = resourceService.createResource(request, institutionId, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Resource created successfully", response));
    }

    @GetMapping("/{resourceId}")
    @Operation(summary = "Get a specific resource")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<ResourceResponse>> getResource(
            @PathVariable UUID resourceId,
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute("userRole") String userRole) {
        ResourceResponse response = resourceService.getResource(resourceId, institutionId, userId, userRole);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "List resources")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<List<ResourceResponse>>> listResources(
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute("userRole") String userRole,
            @RequestParam(required = false) UUID lessonId,
            @RequestParam(required = false) UUID moduleId,
            @RequestParam(required = false) UUID courseId,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String visibility,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        List<ResourceResponse> resources = resourceService.listResources(
                institutionId, userId, userRole, lessonId, moduleId, courseId, resourceType, visibility, page, size);
        return ResponseEntity.ok(ApiResponse.success(resources));
    }

    @PutMapping("/{resourceId}")
    @Operation(summary = "Update a resource")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<ResourceResponse>> updateResource(
            @PathVariable UUID resourceId,
            @Valid @RequestBody ResourceRequest request,
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute("userRole") String userRole) {
        ResourceResponse response = resourceService.updateResource(resourceId, request, institutionId, userId, userRole);
        return ResponseEntity.ok(ApiResponse.success("Resource updated successfully", response));
    }

    @DeleteMapping("/{resourceId}")
    @Operation(summary = "Delete a resource")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<Void>> deleteResource(
            @PathVariable UUID resourceId,
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute("userRole") String userRole) {
        resourceService.deleteResource(resourceId, institutionId, userId, userRole);
        return ResponseEntity.ok(ApiResponse.success("Resource deleted successfully", null));
    }

    @PostMapping("/{resourceId}/tags")
    @Operation(summary = "Add a tag to a resource")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<ResourceResponse>> addTag(
            @PathVariable UUID resourceId,
            @RequestParam String tagName,
            @RequestAttribute("institutionId") UUID institutionId) {
        ResourceResponse response = resourceService.addTag(resourceId, tagName, institutionId);
        return ResponseEntity.ok(ApiResponse.success("Tag added successfully", response));
    }

    @DeleteMapping("/{resourceId}/tags/{tagId}")
    @Operation(summary = "Remove a tag from a resource")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<Void>> removeTag(
            @PathVariable UUID resourceId,
            @PathVariable UUID tagId,
            @RequestAttribute("institutionId") UUID institutionId) {
        resourceService.removeTag(resourceId, tagId, institutionId);
        return ResponseEntity.ok(ApiResponse.success("Tag removed successfully", null));
    }

    @PostMapping("/{resourceId}/save")
    @Operation(summary = "Save a resource to student's library")
    @PreAuthorize("hasAnyRole('STUDENT')")
    public ResponseEntity<ApiResponse<Void>> saveResource(
            @PathVariable UUID resourceId,
            @RequestAttribute("userId") UUID userId) {
        resourceService.saveResource(userId, resourceId);
        return ResponseEntity.ok(ApiResponse.success("Resource saved successfully", null));
    }

    @DeleteMapping("/{resourceId}/save")
    @Operation(summary = "Unsave a resource from student's library")
    @PreAuthorize("hasAnyRole('STUDENT')")
    public ResponseEntity<ApiResponse<Void>> unsaveResource(
            @PathVariable UUID resourceId,
            @RequestAttribute("userId") UUID userId) {
        resourceService.unsaveResource(userId, resourceId);
        return ResponseEntity.ok(ApiResponse.success("Resource unsaved successfully", null));
    }

    @GetMapping("/saved")
    @Operation(summary = "Get student's saved resources")
    @PreAuthorize("hasAnyRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<ResourceResponse>>> getSavedResources(
            @RequestAttribute("userId") UUID userId) {
        List<ResourceResponse> resources = resourceService.getSavedResources(userId);
        return ResponseEntity.ok(ApiResponse.success(resources));
    }

    @GetMapping("/{resourceId}/saved")
    @Operation(summary = "Check if resource is saved by student")
    @PreAuthorize("hasAnyRole('STUDENT')")
    public ResponseEntity<ApiResponse<Boolean>> isSaved(
            @PathVariable UUID resourceId,
            @RequestAttribute("userId") UUID userId) {
        boolean saved = resourceService.isSaved(userId, resourceId);
        return ResponseEntity.ok(ApiResponse.success(saved));
    }

    // ── Annotations ──

    @GetMapping("/{resourceId}/annotations")
    @Operation(summary = "List annotations for a resource")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<List<ResourceAnnotationResponse>>> listAnnotations(
            @PathVariable UUID resourceId,
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestAttribute("userRole") String userRole) {
        List<ResourceAnnotationResponse> annotations =
                annotationService.listAnnotations(resourceId, institutionId, userRole);
        return ResponseEntity.ok(ApiResponse.success(annotations));
    }

    @PostMapping("/{resourceId}/annotations")
    @Operation(summary = "Create an annotation on a resource")
    @PreAuthorize("hasAnyRole('STUDENT')")
    public ResponseEntity<ApiResponse<ResourceAnnotationResponse>> createAnnotation(
            @PathVariable UUID resourceId,
            @Valid @RequestBody ResourceAnnotationRequest request,
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestAttribute("userId") UUID userId) {
        ResourceAnnotationResponse response =
                annotationService.createAnnotation(resourceId, request, institutionId, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Annotation created successfully", response));
    }

    @DeleteMapping("/{resourceId}/annotations/{annotationId}")
    @Operation(summary = "Delete an annotation")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<Void>> deleteAnnotation(
            @PathVariable UUID resourceId,
            @PathVariable UUID annotationId,
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestAttribute("userId") UUID userId) {
        annotationService.deleteAnnotation(annotationId, institutionId, userId);
        return ResponseEntity.ok(ApiResponse.success("Annotation deleted successfully", null));
    }

    @GetMapping("/my-annotations")
    @Operation(summary = "Get current student's annotations")
    @PreAuthorize("hasAnyRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<ResourceAnnotationResponse>>> getMyAnnotations(
            @RequestAttribute("userId") UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(annotationService.listMyAnnotations(userId)));
    }

    // ── Analytics ──

    @GetMapping("/{resourceId}/analytics")
    @Operation(summary = "Get resource analytics summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<ResourceAnalyticsSummary>> getAnalyticsSummary(
            @PathVariable UUID resourceId,
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        ResourceAnalyticsSummary summary =
                analyticsService.getSummary(resourceId, institutionId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/{resourceId}/analytics/daily")
    @Operation(summary = "Get daily resource analytics rows")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<ResourceAnalytics>>> getAnalyticsDaily(
            @PathVariable UUID resourceId,
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        List<ResourceAnalytics> rows =
                analyticsService.getDaily(resourceId, institutionId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(rows));
    }

    @PostMapping("/{resourceId}/analytics/view")
    @Operation(summary = "Record a resource view")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<ResourceAnalyticsSummary>> recordView(
            @PathVariable UUID resourceId,
            @RequestAttribute("institutionId") UUID institutionId) {
        return ResponseEntity.ok(ApiResponse.success(
                "View recorded", analyticsService.recordView(resourceId, institutionId)));
    }

    @PostMapping("/{resourceId}/analytics/download")
    @Operation(summary = "Record a resource download")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<ResourceAnalyticsSummary>> recordDownload(
            @PathVariable UUID resourceId,
            @RequestAttribute("institutionId") UUID institutionId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Download recorded", analyticsService.recordDownload(resourceId, institutionId)));
    }
}