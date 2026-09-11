package tz.elmkusoma.parent.controller;

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
import tz.elmkusoma.parent.dto.request.LinkStudentRequest;
import tz.elmkusoma.parent.dto.request.ParentNotificationPreferenceRequest;
import tz.elmkusoma.parent.dto.request.ParentRequest;
import tz.elmkusoma.parent.dto.response.ParentNotificationPreferenceResponse;
import tz.elmkusoma.parent.dto.response.ParentResponse;
import tz.elmkusoma.parent.dto.response.ParentStudentResponse;
import tz.elmkusoma.parent.service.ParentService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/parents")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER','INSTITUTION_ADMIN','ADMIN')")
@Tag(name = "Parent Management", description = "CRUD operations for parents and parent-student linkages")
public class ParentController {

    private final ParentService parentService;

    @PostMapping
    @Operation(summary = "Create a new parent profile")
    public ResponseEntity<ApiResponse<ParentResponse>> createParent(
            @RequestAttribute("institutionId") UUID institutionId,
            @Valid @RequestBody ParentRequest request) {
        ParentResponse parent = parentService.createParent(institutionId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Parent created successfully", parent));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a parent by ID")
    public ResponseEntity<ApiResponse<ParentResponse>> getParent(
            @RequestAttribute("institutionId") UUID institutionId,
            @PathVariable UUID id) {
        ParentResponse parent = parentService.getParent(institutionId, id);
        return ResponseEntity.ok(ApiResponse.success(parent));
    }

    @GetMapping
    @Operation(summary = "List all parents in an institution")
    public ResponseEntity<ApiResponse<PageResponse<ParentResponse>>> listParents(
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<ParentResponse> parents = parentService.listParents(institutionId, page, size);
        return ResponseEntity.ok(ApiResponse.success(parents));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a parent profile")
    public ResponseEntity<ApiResponse<ParentResponse>> updateParent(
            @RequestAttribute("institutionId") UUID institutionId,
            @PathVariable UUID id,
            @Valid @RequestBody ParentRequest request) {
        ParentResponse parent = parentService.updateParent(institutionId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Parent updated successfully", parent));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a parent")
    public ResponseEntity<ApiResponse<Void>> deleteParent(
            @RequestAttribute("institutionId") UUID institutionId,
            @PathVariable UUID id) {
        parentService.deleteParent(institutionId, id);
        return ResponseEntity.ok(ApiResponse.success("Parent deleted successfully", null));
    }

    @PostMapping("/{id}/link-student")
    @Operation(summary = "Link a student to a parent")
    public ResponseEntity<ApiResponse<ParentStudentResponse>> linkStudent(
            @RequestAttribute("institutionId") UUID institutionId,
            @PathVariable UUID id,
            @Valid @RequestBody LinkStudentRequest request) {
        ParentStudentResponse link = parentService.linkStudent(institutionId, id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Student linked successfully", link));
    }

    @GetMapping("/{id}/children")
    @Operation(summary = "Get all children linked to a parent")
    public ResponseEntity<ApiResponse<List<ParentStudentResponse>>> getChildren(
            @RequestAttribute("institutionId") UUID institutionId,
            @PathVariable UUID id) {
        List<ParentStudentResponse> children = parentService.getChildren(institutionId, id);
        return ResponseEntity.ok(ApiResponse.success(children));
    }

    @DeleteMapping("/links/{linkId}")
    @Operation(summary = "Unlink a student from a parent")
    public ResponseEntity<ApiResponse<Void>> unlinkStudent(
            @RequestAttribute("institutionId") UUID institutionId,
            @PathVariable UUID linkId) {
        parentService.unlinkStudent(institutionId, linkId);
        return ResponseEntity.ok(ApiResponse.success("Student unlinked successfully", null));
    }

    @GetMapping("/{id}/notification-preferences")
    @Operation(summary = "Get parent notification preferences")
    public ResponseEntity<ApiResponse<ParentNotificationPreferenceResponse>> getNotificationPreferences(
            @RequestAttribute("institutionId") UUID institutionId,
            @PathVariable UUID id) {
        ParentNotificationPreferenceResponse prefs = parentService.getNotificationPreferences(institutionId, id);
        return ResponseEntity.ok(ApiResponse.success(prefs));
    }

    @PutMapping("/{id}/notification-preferences")
    @Operation(summary = "Update parent notification preferences")
    public ResponseEntity<ApiResponse<ParentNotificationPreferenceResponse>> updateNotificationPreferences(
            @RequestAttribute("institutionId") UUID institutionId,
            @PathVariable UUID id,
            @Valid @RequestBody ParentNotificationPreferenceRequest request) {
        ParentNotificationPreferenceResponse prefs = parentService.updateNotificationPreferences(institutionId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Notification preferences updated successfully", prefs));
    }
}
