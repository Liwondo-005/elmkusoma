package tz.elmkusoma.bundle.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.bundle.domain.Bundle;
import tz.elmkusoma.bundle.domain.BundleItem;
import tz.elmkusoma.bundle.repository.BundleItemRepository;
import tz.elmkusoma.bundle.repository.BundleRepository;
import tz.elmkusoma.bundle.service.BundleAdminService;
import tz.elmkusoma.bundle.service.BundleAdminService.BundleDetail;

import java.util.List;
import java.util.UUID;

/**
 * Creating, editing and publishing bundles.
 *
 * <p>Institution scope is validated before every operation, not taken from the request body. The
 * institution is a header attribute set by the security layer, so a caller cannot create a bundle
 * inside somebody else's institution by supplying a different id.
 *
 * <p>There is no delete. A bundle that has been bought cannot be un-bought, so removing the row
 * would orphan entitlements that learners already hold. Suspension exists instead and is the
 * correct way to stop a bundle being sold.
 */
@RestController
@RequestMapping("/v1/institution/bundles")
@RequiredArgsConstructor
@Tag(name = "Bundle Administration")
@PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
public class BundleAdminController {

    private final BundleAdminService bundleAdminService;

    @Getter
    @Setter
    public static class BundleItemRequest {
        @NotBlank private String serviceType;
        @NotNull private UUID serviceId;
    }

    @Getter
    @Setter
    public static class BundleRequest {
        @NotBlank private String name;
        private String description;
        /** The pricing key. The price itself is never accepted from the client. */
        @NotBlank private String serviceType;
        private Integer accessDays;
        private List<BundleItemRequest> items;
    }

    @GetMapping
    @Operation(summary = "All bundles in this institution, including unpublished ones.")
    public ResponseEntity<ApiResponse<List<Bundle>>> list(
            @RequestAttribute UUID institutionId,
            @RequestAttribute("userId") UUID userId) {
        bundleAdminService.requireScope(userId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(
                bundleAdminService.listForInstitution(institutionId)));
    }

    @GetMapping("/{bundleId}")
    @Operation(summary = "One bundle in this institution and the services it contains, "
            + "including unpublished ones.")
    public ResponseEntity<ApiResponse<BundleDetail>> detail(
            @RequestAttribute UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID bundleId) {
        bundleAdminService.requireScope(userId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(
                bundleAdminService.detail(institutionId, bundleId)));
    }

    @PostMapping
    @Operation(summary = "Create a draft bundle with its items.")
    public ResponseEntity<ApiResponse<Bundle>> create(
            @RequestAttribute UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @RequestBody BundleRequest request) {
        bundleAdminService.requireScope(userId, institutionId);
        Bundle created = bundleAdminService.create(institutionId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created));
    }

    @PutMapping("/{bundleId}")
    @Operation(summary = "Replace a draft bundle's name, description, duration and items.")
    public ResponseEntity<ApiResponse<Bundle>> update(
            @RequestAttribute UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID bundleId,
            @RequestBody BundleRequest request) {
        bundleAdminService.requireScope(userId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(
                bundleAdminService.update(institutionId, bundleId, request)));
    }

    @PostMapping("/{bundleId}/publish")
    @Operation(summary = "Publish a draft bundle so it becomes purchasable.")
    public ResponseEntity<ApiResponse<Bundle>> publish(
            @RequestAttribute UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID bundleId) {
        bundleAdminService.requireScope(userId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(
                bundleAdminService.publish(institutionId, bundleId)));
    }

    @PostMapping("/{bundleId}/suspend")
    @Operation(summary = "Stop a bundle being sold. Existing entitlements are not revoked.")
    public ResponseEntity<ApiResponse<Bundle>> suspend(
            @RequestAttribute UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID bundleId) {
        bundleAdminService.requireScope(userId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(
                bundleAdminService.suspend(institutionId, bundleId)));
    }

    @DeleteMapping("/{bundleId}/items/{itemId}")
    @Operation(summary = "Remove one item from a draft bundle.")
    public ResponseEntity<ApiResponse<Void>> removeItem(
            @RequestAttribute UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID bundleId,
            @PathVariable UUID itemId) {
        bundleAdminService.requireScope(userId, institutionId);
        bundleAdminService.removeItem(institutionId, bundleId, itemId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}