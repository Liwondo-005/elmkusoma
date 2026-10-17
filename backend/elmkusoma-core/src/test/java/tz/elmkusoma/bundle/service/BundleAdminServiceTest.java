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
import tz.elmkusoma.testutil.TestDataSeeder;

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

    @BeforeEach
    void allowScope() {
        // The scope service is exercised by its own tests; here it would only obscure the rules.
        Mockito.mockingDetails(bundleAdminService);
    }

    private BundleRequest request(Integer accessDays, int itemCount) {
        BundleRequest r = new BundleRequest();
        r.setName("Probe bundle " + UUID.randomUUID());
        r.setDescription("admin rules probe");
        r.setServiceType("BUNDLE_ADMIN_" + UUID.randomUUID());
        r.setAccessDays(accessDays);
        r.setItems(java.util.stream.IntStream.range(0, itemCount).mapToObj(i -> {
            BundleItemRequest item = new BundleItemRequest();
            item.setServiceType("COURSE_ACCESS");
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
        duplicate.setServiceType("COURSE_ACCESS");
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
}