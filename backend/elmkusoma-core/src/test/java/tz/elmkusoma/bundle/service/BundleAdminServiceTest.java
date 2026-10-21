package tz.elmkusoma.bundle.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import tz.elmkusoma.administration.service.InstitutionScopeService;
import tz.elmkusoma.bundle.controller.BundleAdminController.BundleItemRequest;
import tz.elmkusoma.bundle.controller.BundleAdminController.BundleRequest;
import tz.elmkusoma.bundle.domain.Bundle;
import tz.elmkusoma.bundle.domain.BundleItem;
import tz.elmkusoma.bundle.repository.BundleItemRepository;
import tz.elmkusoma.bundle.repository.BundleRepository;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.pricing.domain.PricingPolicy;
import tz.elmkusoma.pricing.repository.PricingPolicyRepository;
import tz.elmkusoma.testutil.TestDataSeeder;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Authoring bundles is where a mistake becomes a promise: publish something empty, publish access
 * with no end date, or edit a bundle buyers have already paid for. These tests pin the rules that
 * prevent each of those, and pin that a bundle id from another institution does not resolve.
 */
@SpringBootTest
@Import(TestDataSeeder.class)
@TestPropertySource(locations = "classpath:application-test.properties")
class BundleAdminServiceTest {

    @Autowired private BundleAdminService bundleAdminService;
    @Autowired private BundleRepository bundleRepository;
    @Autowired private BundleItemRepository bundleItemRepository;
    @Autowired private PricingPolicyRepository pricingPolicyRepository;

    /**
     * The catalogue a bundle item is validated against. Items must name a service the platform
     * actually sells, and "actually sells" is decided by an ACTIVE pricing policy - so the probe
     * fixture registers one rather than relying on a name nothing resolves.
     */
    private static final String PRICED_SERVICE = "BUNDLE_PROBE_SERVICE";

    @BeforeEach
    void allowScope() {
        // The scope service is exercised by its own tests; here it would only obscure the rules.
        Mockito.mockingDetails(bundleAdminService);
        if (!pricingPolicyRepository.existsByServiceTypeAndStatusAndIsDeletedFalse(
                PRICED_SERVICE, PricingPolicy.STATUS_ACTIVE)) {
            pricingPolicyRepository.save(PricingPolicy.builder()
                    .policyKey("SERVICE_DEFAULT")
                    .policyVersion(1)
                    .status(PricingPolicy.STATUS_ACTIVE)
                    .serviceType(PRICED_SERVICE)
                    .defaultAmount(new BigDecimal("1000.00"))
                    .currency("TZS")
                    .build());
        }
    }

    private BundleRequest request(Integer accessDays, int itemCount) {
        BundleRequest r = new BundleRequest();
        r.setName("Probe bundle " + UUID.randomUUID());
        r.setDescription("admin rules probe");
        r.setServiceType("BUNDLE_ADMIN_" + UUID.randomUUID());
        r.setAccessDays(accessDays);
        r.setItems(java.util.stream.IntStream.range(0, itemCount).mapToObj(i -> {
            BundleItemRequest item = new BundleItemRequest();
            item.setServiceType(PRICED_SERVICE);
            item.setServiceId(UUID.randomUUID());
            return item;
        }).toList());
        return r;
    }

    @Test
    void aNewBundleIsAlwaysADraft() {
        Bundle created = bundleAdminService.create(
                TestDataSeeder.INSTITUTION_ID, UUID.randomUUID(), request(30, 2));

        assertEquals(Bundle.STATUS_DRAFT, created.getStatus(),
                "a bundle must never come into existence already purchasable; publishing is a "
                        + "separate deliberate act");
        assertEquals(2, bundleItemRepository
                .findAllByBundleIdAndIsDeletedFalseOrderByPositionAsc(created.getId()).size());
    }

    @Test
    void anEmptyBundleIsRefusedAtCreation() {
        assertThrows(IllegalArgumentException.class, () -> bundleAdminService.create(
                        TestDataSeeder.INSTITUTION_ID, UUID.randomUUID(), request(30, 0)),
                "a bundle that would sell nothing must not be creatable");
    }

    @Test
    void theSameServiceCannotBeListedTwice() {
        BundleRequest r = request(30, 1);
        UUID shared = UUID.randomUUID();
        r.getItems().get(0).setServiceId(shared);
        BundleItemRequest duplicate = new BundleItemRequest();
        duplicate.setServiceType(PRICED_SERVICE);
        duplicate.setServiceId(shared);
        r.setItems(List.of(r.getItems().get(0), duplicate));

        assertThrows(IllegalArgumentException.class, () -> bundleAdminService.create(
                        TestDataSeeder.INSTITUTION_ID, UUID.randomUUID(), r),
                "listing a service twice would grant it twice on purchase");
    }

    @Test
    void publishingRefusesABundleWithNoDuration() {
        // The database enforces this too, but the service must refuse with an explanation first.
        Bundle created = bundleAdminService.create(
                TestDataSeeder.INSTITUTION_ID, UUID.randomUUID(), request(null, 1));

        assertThrows(IllegalStateException.class, () -> bundleAdminService.publish(
                        TestDataSeeder.INSTITUTION_ID, created.getId()),
                "access with no end date must not be sold");
    }

    @Test
    void publishingMakesABundlePurchasableAndSuspensionStopsTheSaleAgain() {
        Bundle created = bundleAdminService.create(
                TestDataSeeder.INSTITUTION_ID, UUID.randomUUID(), request(30, 1));

        Bundle published = bundleAdminService.publish(TestDataSeeder.INSTITUTION_ID, created.getId());
        assertEquals(Bundle.STATUS_PUBLISHED, published.getStatus());
        assertTrue(published.isPurchasable());

        Bundle suspended = bundleAdminService.suspend(TestDataSeeder.INSTITUTION_ID, created.getId());
        assertEquals(Bundle.STATUS_SUSPENDED, suspended.getStatus());
        assertTrue(!suspended.isPurchasable(), "a suspended bundle must not be purchasable");
    }

    @Test
    void aPublishedBundleCannotBeEdited() {
        Bundle created = bundleAdminService.create(
                TestDataSeeder.INSTITUTION_ID, UUID.randomUUID(), request(30, 1));
        bundleAdminService.publish(TestDataSeeder.INSTITUTION_ID, created.getId());

        assertThrows(IllegalStateException.class, () -> bundleAdminService.update(
                        TestDataSeeder.INSTITUTION_ID, created.getId(), request(30, 2)),
                "editing a published bundle would change what buyers were promised after paying");
        assertThrows(IllegalStateException.class, () -> bundleAdminService.publish(
                        TestDataSeeder.INSTITUTION_ID, created.getId()),
                "publishing twice is not a state transition");
    }

    @Test
    void editingADraftLoadsTheItemsItActuallyContains() {
        // The admin editor opens a bundle and then saves the whole item set back. If detail did
        // not return the items, every save would delete them and the bundle would sell nothing,
        // so this is the contract that makes editing safe at all.
        BundleRequest r = request(45, 3);
        Bundle created = bundleAdminService.create(
                TestDataSeeder.INSTITUTION_ID, UUID.randomUUID(), r);

        BundleAdminService.BundleDetail detail = bundleAdminService.detail(
                TestDataSeeder.INSTITUTION_ID, created.getId());

        assertEquals(created.getId(), detail.bundle().getId());
        assertEquals(Bundle.STATUS_DRAFT, detail.bundle().getStatus(),
                "an administrator must be able to open a draft; the public catalog refuses one on "
                        + "purpose, so this is the only way to see what a draft holds");
        assertEquals(3, detail.items().size(),
                "every authored item must be returned, or saving the editor would erase them");
        for (int i = 0; i < 3; i++) {
            assertEquals(r.getItems().get(i).getServiceType(), detail.items().get(i).getServiceType());
            assertEquals(r.getItems().get(i).getServiceId(), detail.items().get(i).getServiceId());
            assertEquals(i, detail.items().get(i).getPosition(),
                    "items keep their authored order so the editor does not reshuffle the bundle");
        }
    }

    @Test
    void editingADraftDoesNotReturnRemovedItems() {
        Bundle created = bundleAdminService.create(
                TestDataSeeder.INSTITUTION_ID, UUID.randomUUID(), request(30, 2));
        BundleItem toRemove = bundleItemRepository
                .findAllByBundleIdAndIsDeletedFalseOrderByPositionAsc(created.getId())
                .get(0);
        bundleAdminService.removeItem(
                TestDataSeeder.INSTITUTION_ID, created.getId(), toRemove.getId());

        assertEquals(1, bundleAdminService.detail(
                        TestDataSeeder.INSTITUTION_ID, created.getId()).items().size(),
                "a soft-deleted item must not be offered to the editor as if it were still there; "
                        + "saving the editor would otherwise write it back");
    }

    @Test
    void aBundleInAnotherInstitutionDoesNotResolve() {
        Bundle foreign = bundleAdminService.create(
                UUID.randomUUID(), UUID.randomUUID(), request(30, 1));

        assertThrows(ResourceNotFoundException.class, () -> bundleAdminService.publish(
                        TestDataSeeder.INSTITUTION_ID, foreign.getId()),
                "a bundle id from another institution must not resolve through this institution");
    }

    @Test
    void removingAnItemRequiresADraftAndTheRightBundle() {
        Bundle a = bundleAdminService.create(
                TestDataSeeder.INSTITUTION_ID, UUID.randomUUID(), request(30, 2));
        Bundle b = bundleAdminService.create(
                TestDataSeeder.INSTITUTION_ID, UUID.randomUUID(), request(30, 1));
        BundleItem itemOfB = bundleItemRepository
                .findAllByBundleIdAndIsDeletedFalseOrderByPositionAsc(b.getId()).get(0);

        assertThrows(tz.elmkusoma.exception.ForbiddenException.class,
                () -> bundleAdminService.removeItem(
                        TestDataSeeder.INSTITUTION_ID, a.getId(), itemOfB.getId()),
                "an item from another bundle must not be removable through this bundle's URL");

        bundleAdminService.removeItem(TestDataSeeder.INSTITUTION_ID, a.getId(),
                bundleItemRepository.findAllByBundleIdAndIsDeletedFalseOrderByPositionAsc(a.getId())
                        .get(0).getId());
        assertEquals(1, bundleItemRepository
                .findAllByBundleIdAndIsDeletedFalseOrderByPositionAsc(a.getId()).size(),
                "the removed item must no longer be part of the bundle");
    }

    // ---------------------------------------------------------------------------------------------
    // B-10: an item must name a service the platform actually sells.
    //
    // bundle_items.service_id has no foreign key, and the grant copied it straight onto an ACTIVE
    // entitlement. An item pointing at a UUID that belongs to no course, resource or event
    // therefore granted access to nothing while still counting as a purchase. The trusted catalogue
    // is the pricing catalogue, because the same ACTIVE policy is what BundleTopUpService resolves
    // the item's price from - so an unpriced item is not merely dangling, it is unsellable.
    // ---------------------------------------------------------------------------------------------

    @Test
    void anItemNamingAServiceThePlatformDoesNotSellIsRefused() {
        BundleRequest r = request(30, 1);
        r.getItems().get(0).setServiceType("NOT_A_SERVICE_ANYONE_SELLS");

        IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                () -> bundleAdminService.create(
                        TestDataSeeder.INSTITUTION_ID, UUID.randomUUID(), r),
                "an item pointing at nothing would grant access to nothing while still looking "
                        + "like a purchase");
        assertTrue(refused.getMessage().contains("NOT_A_SERVICE_ANYONE_SELLS"),
                "the refusal must name the offending service so the admin knows what to fix, "
                        + "rather than reporting a generic failure");
        assertEquals(0, bundleRepository
                        .findAllByInstitutionIdAndIsDeletedFalseOrderByNameAsc(
                                TestDataSeeder.INSTITUTION_ID).stream()
                        .filter(b -> b.getName().equals(r.getName())).count(),
                "nothing may be persisted when an item fails the catalogue check");
    }

    @Test
    void aDraftCannotBePublishedOnceItsServiceIsNoLongerSold() {
        Bundle created = bundleAdminService.create(
                TestDataSeeder.INSTITUTION_ID, UUID.randomUUID(), request(30, 1));

        // The policy is retired between drafting and publishing - exactly the window where a
        // draft would otherwise go on sale naming a service the platform no longer prices.
        PricingPolicy policy = pricingPolicyRepository
                .findFirstByServiceTypeAndStatusAndIsDeletedFalseOrderByPolicyVersionDesc(
                        PRICED_SERVICE, PricingPolicy.STATUS_ACTIVE)
                .orElseThrow();
        policy.setStatus(PricingPolicy.STATUS_RETIRED);
        pricingPolicyRepository.save(policy);

        assertThrows(IllegalArgumentException.class, () -> bundleAdminService.publish(
                        TestDataSeeder.INSTITUTION_ID, created.getId()),
                "publication is the activation gate; the catalogue must be re-checked there even "
                        + "though it was checked when the item was written");
    }

    @Test
    void anEmptyServiceNameIsRefusedRatherThanTreatedAsAValidType() {
        BundleRequest r = request(30, 1);
        r.getItems().get(0).setServiceType("   ");

        assertThrows(IllegalArgumentException.class, () -> bundleAdminService.create(
                        TestDataSeeder.INSTITUTION_ID, UUID.randomUUID(), r),
                "a blank service type is not a service the platform sells");
    }
}