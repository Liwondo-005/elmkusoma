package tz.elmkusoma.bundle.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.bundle.controller.BundleAdminController.BundleItemRequest;
import tz.elmkusoma.bundle.controller.BundleAdminController.BundleRequest;
import tz.elmkusoma.bundle.domain.Bundle;
import tz.elmkusoma.bundle.domain.BundleItem;
import tz.elmkusoma.bundle.repository.BundleItemRepository;
import tz.elmkusoma.bundle.repository.BundleRepository;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.administration.service.InstitutionScopeService;
import tz.elmkusoma.pricing.domain.PricingPolicy;
import tz.elmkusoma.pricing.repository.PricingPolicyRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Authoring bundles.
 *
 * <p>Every operation resolves the bundle through the caller's institution rather than trusting the
 * bundle id alone, so an id guessed or copied from another institution simply does not resolve.
 *
 * <p>Only a DRAFT bundle may be edited. Editing a published bundle would change what buyers were
 * promised after they paid, and suspension exists precisely so a sold bundle can be stopped without
 * rewriting it.
 */
@Service
@RequiredArgsConstructor
public class BundleAdminService {

    private final BundleRepository bundleRepository;
    private final BundleItemRepository bundleItemRepository;
    private final InstitutionScopeService scopeService;
    private final PricingPolicyRepository pricingPolicyRepository;

    /** Fails closed before anything is read or written. */
    public void requireScope(UUID userId, UUID institutionId) {
        scopeService.validateAccess(userId, institutionId, "manage_bundles");
    }

    @Transactional(readOnly = true)
    public List<Bundle> listForInstitution(UUID institutionId) {
        return bundleRepository.findAllByInstitutionIdAndIsDeletedFalseOrderByNameAsc(institutionId);
    }

    /**
     * One bundle with the services it contains, resolved inside the caller's institution.
     *
     * <p>Drafts are included deliberately. An administrator has to be able to see what is inside a
     * draft before deciding to publish it, and the public catalog refuses drafts on purpose - so
     * without this the editor would be editing a bundle whose contents it cannot see. Every bundle
     * item is inherited from a specific target, so getting the item list wrong is not cosmetic:
     * {@code update} replaces the whole set, which means an editor that could not load the items
     * would delete them by saving.
     */
    @Transactional(readOnly = true)
    public BundleDetail detail(UUID institutionId, UUID bundleId) {
        Bundle bundle = ownedBundle(institutionId, bundleId);
        return new BundleDetail(bundle, bundleItemRepository
                .findAllByBundleIdAndIsDeletedFalseOrderByPositionAsc(bundle.getId()));
    }

    /** A bundle plus its contents, in the order they were authored. */
    public record BundleDetail(Bundle bundle, List<BundleItem> items) {
    }

    @Transactional
    public Bundle create(UUID institutionId, UUID userId, BundleRequest request) {
        // A bundle with nothing in it can never grant access. Refusing at creation is better than
        // publishing something that sells an empty box.
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("A bundle must contain at least one item");
        }
        requireDistinctItems(request.getItems());
        requirePricedServices(serviceTypesOf(request.getItems()));

        Bundle bundle = bundleRepository.saveAndFlush(Bundle.builder()
                .institutionId(institutionId)
                .name(request.getName())
                .description(request.getDescription())
                .serviceType(request.getServiceType().trim())
                .accessDays(request.getAccessDays())
                // Always starts as a draft. Publishing is a separate, deliberate action.
                .status(Bundle.STATUS_DRAFT)
                .createdBy(String.valueOf(userId))
                .build());

        writeItems(bundle, request.getItems());
        return bundle;
    }

    @Transactional
    public Bundle update(UUID institutionId, UUID bundleId, BundleRequest request) {
        Bundle bundle = ownedBundle(institutionId, bundleId);
        requireDraft(bundle, "edited");
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("A bundle must contain at least one item");
        }
        requireDistinctItems(request.getItems());
        requirePricedServices(serviceTypesOf(request.getItems()));

        bundle.setName(request.getName());
        bundle.setDescription(request.getDescription());
        bundle.setServiceType(request.getServiceType().trim());
        bundle.setAccessDays(request.getAccessDays());

        // Replacing the whole item set keeps the display honest: a removed item is not still
        // hiding in the bundle waiting to be granted to the next buyer.
        for (BundleItem existing : bundleItemRepository
                .findAllByBundleIdAndIsDeletedFalseOrderByPositionAsc(bundle.getId())) {
            existing.setIsDeleted(true);
            bundleItemRepository.save(existing);
        }
        Bundle saved = bundleRepository.saveAndFlush(bundle);
        writeItems(saved, request.getItems());
        return saved;
    }

    @Transactional
    public Bundle publish(UUID institutionId, UUID bundleId) {
        Bundle bundle = ownedBundle(institutionId, bundleId);
        requireDraft(bundle, "published");

        List<BundleItem> items = bundleItemRepository
                .findAllByBundleIdAndIsDeletedFalseOrderByPositionAsc(bundleId);
        if (items.isEmpty()) {
            throw new IllegalStateException(
                    "A bundle with no items cannot be published; it would sell nothing");
        }
        if (bundle.getAccessDays() == null || bundle.getAccessDays() <= 0) {
            // Publishing without a duration would sell access with an unknown end date.
            throw new IllegalStateException(
                    "A bundle needs an access duration before it can be published");
        }
        // Publication is the activation gate, so the catalogue is re-checked here even though it
        // was checked when the items were written: a policy can be retired between drafting and
        // publishing, and a bundle must never go on sale naming a service that is no longer priced.
        requirePricedServices(items.stream().map(BundleItem::getServiceType).toList());
        bundle.setStatus(Bundle.STATUS_PUBLISHED);
        return bundleRepository.saveAndFlush(bundle);
    }

    @Transactional
    public Bundle suspend(UUID institutionId, UUID bundleId) {
        Bundle bundle = ownedBundle(institutionId, bundleId);
        // Suspension stops the sale. It deliberately does not revoke entitlements already granted:
        // somebody paid for those, and removing access is a refund decision, not a toggle.
        bundle.setStatus(Bundle.STATUS_SUSPENDED);
        return bundleRepository.saveAndFlush(bundle);
    }

    @Transactional
    public void removeItem(UUID institutionId, UUID bundleId, UUID itemId) {
        Bundle bundle = ownedBundle(institutionId, bundleId);
        requireDraft(bundle, "edited");
        BundleItem item = bundleItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("BundleItem", "id", itemId));
        if (!item.getBundleId().equals(bundleId)) {
            // An item id from another bundle must not be removable through this bundle's URL.
            throw new ForbiddenException("That item does not belong to this bundle");
        }
        item.setIsDeleted(true);
        bundleItemRepository.save(item);
    }

    private Bundle ownedBundle(UUID institutionId, UUID bundleId) {
        Bundle bundle = bundleRepository.findById(bundleId)
                .orElseThrow(() -> new ResourceNotFoundException("Bundle", "id", bundleId));
        if (Boolean.TRUE.equals(bundle.getIsDeleted())) {
            throw new ResourceNotFoundException("Bundle", "id", bundleId);
        }
        if (bundle.getInstitutionId() != null
                && !bundle.getInstitutionId().equals(institutionId)) {
            // Resolved inside the institution, never across it.
            throw new ResourceNotFoundException("Bundle", "id", bundleId);
        }
        return bundle;
    }

    private void requireDraft(Bundle bundle, String verb) {
        if (!Bundle.STATUS_DRAFT.equals(bundle.getStatus())) {
            throw new IllegalStateException("Only a draft bundle may be " + verb
                    + "; this one is " + bundle.getStatus() + ". Suspend it instead if it must "
                    + "stop being sold.");
        }
    }

    private void requireDistinctItems(List<BundleItemRequest> items) {
        Set<String> seen = new HashSet<>();
        for (BundleItemRequest item : items) {
            String key = item.getServiceType() + ":" + item.getServiceId();
            if (!seen.add(key)) {
                throw new IllegalArgumentException(
                        "A bundle cannot contain the same service twice: " + item.getServiceType());
            }
        }
    }

    private static List<String> serviceTypesOf(List<BundleItemRequest> items) {
        List<String> types = new ArrayList<>();
        for (BundleItemRequest item : items) {
            types.add(item.getServiceType() == null ? "" : item.getServiceType().trim());
        }
        return types;
    }

    /**
     * Every item must name a service the platform actually sells.
     *
     * <p>{@code bundle_items.service_id} carries no foreign key, and nothing else in the write
     * path resolved it, so an item could point at a UUID belonging to no course, resource or event
     * at all. The grant then copied that UUID straight onto an {@code ACTIVE} entitlement, which is
     * access to nothing while still looking like a purchase. The catalogue that can be trusted is
     * the pricing catalogue: a service type the platform sells has an ACTIVE policy, and that same
     * policy is what {@code BundleTopUpService} resolves the item's price from. Checking it here is
     * therefore not a new financial rule - it is the existing price-resolution rule applied at the
     * moment the item is written, and again at publication.</p>
     *
     * <p>Nothing is invented when the catalogue does not contain a name. The refusal names the
     * offending type and says what to do, because the correction is a policy decision the platform
     * admin makes, not one this service can make by guessing a price.</p>
     */
    private void requirePricedServices(List<String> serviceTypes) {
        for (String serviceType : serviceTypes) {
            if (serviceType.isEmpty()) {
                throw new IllegalArgumentException("A bundle item must name a service type");
            }
            if (!pricingPolicyRepository.existsByServiceTypeAndStatusAndIsDeletedFalse(
                    serviceType, PricingPolicy.STATUS_ACTIVE)) {
                throw new IllegalArgumentException(
                        "Bundle item names a service the platform does not currently sell: '"
                                + serviceType + "'. Each item needs an ACTIVE pricing policy, "
                                + "because that policy is what resolves its price and what the "
                                + "entitlement grant keys on. Activate or add the policy first, "
                                + "or name a service the catalogue already prices.");
            }
        }
    }

    private void writeItems(Bundle bundle, List<BundleItemRequest> items) {
        int position = 0;
        for (BundleItemRequest request : items) {
            bundleItemRepository.saveAndFlush(BundleItem.builder()
                    .bundleId(bundle.getId())
                    .serviceType(request.getServiceType().trim())
                    .serviceId(request.getServiceId())
                    .position(position++)
                    .build());
        }
    }
}