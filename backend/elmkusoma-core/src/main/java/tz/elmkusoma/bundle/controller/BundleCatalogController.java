package tz.elmkusoma.bundle.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tz.elmkusoma.bundle.domain.Bundle;
import tz.elmkusoma.bundle.domain.BundleItem;
import tz.elmkusoma.bundle.repository.BundleRepository;
import tz.elmkusoma.bundle.service.BundleGrantService;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.exception.ResourceNotFoundException;

import java.util.List;
import java.util.UUID;

/**
 * What a learner or parent may browse.
 *
 * <p>Only PUBLISHED bundles appear. A draft or suspended bundle is not merely hidden here - the
 * grant path refuses it too, so this endpoint cannot be the only thing standing between an
 * unpublished bundle and a sale.
 *
 * <p>No price is returned. A bundle's price comes from the pricing service at checkout, and a
 * figure quoted in a listing could go stale the moment the policy changes - showing one would
 * invite a purchase at a price that is no longer approved.
 */
@RestController
@RequestMapping("/v1/bundles")
@RequiredArgsConstructor
@Tag(name = "Bundles")
@PreAuthorize("isAuthenticated()")
public class BundleCatalogController {

    private final BundleRepository bundleRepository;
    private final BundleGrantService bundleGrantService;

    @GetMapping
    @Operation(summary = "Published bundles available to browse.")
    public ResponseEntity<ApiResponse<List<Bundle>>> list() {
        return ResponseEntity.ok(ApiResponse.success(
                bundleRepository.findAllByStatusAndIsDeletedFalseOrderByNameAsc(
                        Bundle.STATUS_PUBLISHED)));
    }

    @GetMapping("/{bundleId}")
    @Operation(summary = "One published bundle and the services it contains.")
    public ResponseEntity<ApiResponse<BundleDetail>> detail(@PathVariable UUID bundleId) {
        Bundle bundle = bundleRepository.findById(bundleId)
                .orElseThrow(() -> new ResourceNotFoundException("Bundle", "id", bundleId));
        // Resolved, then filtered: an unpublished bundle is not a 403 telling the caller it exists,
        // it simply does not exist as far as the catalog is concerned.
        if (Boolean.TRUE.equals(bundle.getIsDeleted()) || !bundle.isPurchasable()) {
            throw new ResourceNotFoundException("Bundle", "id", bundleId);
        }
        List<BundleItem> items = bundleGrantService.itemsOf(bundleId);
        return ResponseEntity.ok(ApiResponse.success(new BundleDetail(bundle, items)));
    }

    /** A bundle plus its contents. The item list is what the buyer is actually purchasing. */
    public record BundleDetail(Bundle bundle, List<BundleItem> items) {
    }
}