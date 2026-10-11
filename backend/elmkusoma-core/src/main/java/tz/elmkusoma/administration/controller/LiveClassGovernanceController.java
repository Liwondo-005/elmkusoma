package tz.elmkusoma.administration.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.common.PageResponse;
import tz.elmkusoma.course.domain.LiveClass;
import tz.elmkusoma.shared.dto.LiveClassGovernanceView;
import tz.elmkusoma.shared.service.LiveClassPublicationService;

import java.util.List;
import java.util.UUID;

/**
 * Platform Admin governance over the public live-class listing.
 *
 * <p>Separate from the existing read-only monitor at {@code PlatformAdminController /live-classes},
 * which lists every class for operational awareness. This one governs publication: who may be
 * announced, to whom, and when it comes off the marketing site.</p>
 *
 * <p>Gated by {@code hasRole('ADMIN')}, matching every other {@code /v1/platform-admin/**}
 * controller and the URL-level fence in SecurityConfig. Hiding a button in the admin UI is not
 * authorization; this is.</p>
 */
@RestController
@RequestMapping("/v1/platform-admin/live-classes/governance")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Platform Administration", description = "Public live-class publication governance")
public class LiveClassGovernanceController {

    private final LiveClassPublicationService publicationService;

    @GetMapping
    @Operation(summary = "Publication review queue: filter by publication state and access level")
    public ResponseEntity<ApiResponse<PageResponse<LiveClassGovernanceView>>> queue(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String publicationStatus,
            @RequestParam(required = false) String accessLevel) {
        Page<LiveClass> result = publicationService.reviewQueue(publicationStatus, accessLevel, page, size);
        return ResponseEntity.ok(ApiResponse.success(new PageResponse<>(
                result.getContent().stream().map(LiveClassGovernanceView::from).toList(),
                result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages(),
                result.isFirst(), result.isLast())));
    }

    @GetMapping("/{id}")
    @Operation(summary = "One class with its publication and session state")
    public ResponseEntity<ApiResponse<LiveClassGovernanceView>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(LiveClassGovernanceView.from(publicationService.get(id))));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve a class for publication")
    public ResponseEntity<ApiResponse<LiveClassGovernanceView>> approve(
            @PathVariable UUID id,
            @RequestBody(required = false) ReviewRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Class approved",
                LiveClassGovernanceView.from(publicationService.approve(id, actor(httpRequest),
                        request == null ? null : request.getNote()))));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject a class from public listing, with a reason")
    public ResponseEntity<ApiResponse<LiveClassGovernanceView>> reject(
            @PathVariable UUID id,
            @RequestBody ReviewRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Class rejected",
                LiveClassGovernanceView.from(publicationService.reject(id, actor(httpRequest),
                        request == null ? null : request.getNote()))));
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "Publish a class to the public listing")
    public ResponseEntity<ApiResponse<LiveClassGovernanceView>> publish(
            @PathVariable UUID id, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Class published",
                LiveClassGovernanceView.from(publicationService.publish(id, actor(httpRequest)))));
    }

    @PostMapping("/{id}/unpublish")
    @Operation(summary = "Remove a class from the public listing without deleting it")
    public ResponseEntity<ApiResponse<LiveClassGovernanceView>> unpublish(
            @PathVariable UUID id,
            @RequestBody(required = false) ReviewRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Class removed from public listing",
                LiveClassGovernanceView.from(publicationService.unpublish(id, actor(httpRequest),
                        request == null ? null : request.getNote()))));
    }

    @PostMapping("/{id}/archive")
    @Operation(summary = "Archive a finished class, preserving attendance, recordings and audit history")
    public ResponseEntity<ApiResponse<Void>> archive(
            @PathVariable UUID id,
            @RequestBody(required = false) ReviewRequest request,
            HttpServletRequest httpRequest) {
        publicationService.archive(id, actor(httpRequest), request == null ? null : request.getNote());
        return ResponseEntity.ok(ApiResponse.success("Class archived", null));
    }

    private static String actor(HttpServletRequest request) {
        Object email = request.getAttribute("userEmail");
        return email != null ? email.toString() : "admin";
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewRequest {
        /** Free-text rationale. Required for rejection; optional elsewhere. */
        private String note;
    }
}