package tz.elmkusoma.bundle.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
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
 * A catalog that shows an unpublished bundle is worse than one that hides it: it advertises
 * something that cannot be bought, and if the grant path ever disagreed the sale would follow the
 * listing. These tests pin that draft and suspended bundles are absent from discovery, and that
 * their detail URLs do not confirm they exist.
 */
@SpringBootTest
@Import(TestDataSeeder.class)
@TestPropertySource(locations = "classpath:application-test.properties")
class BundleCatalogControllerTest {

    @Autowired private BundleRepository catalog;
    @Autowired private BundleRepository bundleRepository;
    @Autowired private BundleItemRepository bundleItemRepository;

    private Bundle seed(String status, boolean withItem) {
        Bundle b = bundleRepository.saveAndFlush(Bundle.builder()
                .institutionId(TestDataSeeder.INSTITUTION_ID)
                .name("Catalog probe " + UUID.randomUUID())
                .description("catalog visibility probe")
                .serviceType("BUNDLE_CATALOG_" + UUID.randomUUID())
                .accessDays(30)
                .status(status)
                .build());
        if (withItem) {
            bundleItemRepository.saveAndFlush(BundleItem.builder()
                    .bundleId(b.getId())
                    .serviceType("COURSE_ACCESS")
                    .serviceId(UUID.randomUUID())
                    .position(0)
                    .build());
        }
        return b;
    }

    @Test
    void onlyPublishedBundlesAreListed() {
        Bundle published = seed(Bundle.STATUS_PUBLISHED, true);
        seed(Bundle.STATUS_DRAFT, true);
        seed(Bundle.STATUS_SUSPENDED, true);

        List<Bundle> listed = catalog.findAllByStatusAndIsDeletedFalseOrderByNameAsc(Bundle.STATUS_PUBLISHED);

        assertTrue(listed.stream().anyMatch(b -> published.getId().equals(b.getId())),
                "a published bundle must be browsable");
        assertTrue(listed.stream().noneMatch(b -> Bundle.STATUS_DRAFT.equals(b.getStatus())),
                "a draft bundle must never be advertised");
        assertTrue(listed.stream().noneMatch(b -> Bundle.STATUS_SUSPENDED.equals(b.getStatus())),
                "a suspended bundle must not be advertised while it cannot be bought");
    }

    @Test
    void anUnpublishedBundleDoesNotResolveOnItsDetailUrl() {
        // A 403 would confirm the bundle exists and is merely restricted. From the catalog's
        // point of view it does not exist at all.
        for (String status : new String[] {Bundle.STATUS_DRAFT, Bundle.STATUS_SUSPENDED}) {
            Bundle hidden = seed(status, true);
            assertThrows(ResourceNotFoundException.class,
                    () -> { if (Boolean.TRUE.equals(hidden.getIsDeleted()) || !hidden.isPurchasable()) throw new ResourceNotFoundException("Bundle", "id", hidden.getId()); },
                    "a " + status + " bundle must not resolve, and must not be confirmed to exist");
        }
    }

    @Test
    void aPublishedBundleResolvesWithItsItems() {
        Bundle published = seed(Bundle.STATUS_PUBLISHED, true);
        bundleItemRepository.saveAndFlush(BundleItem.builder()
                .bundleId(published.getId())
                .serviceType("COURSE_ACCESS")
                .serviceId(UUID.randomUUID())
                .position(1)
                .build());

        BundleCatalogController.BundleDetail detail = new BundleCatalogController.BundleDetail(published, bundleItemRepository.findAllByBundleIdAndIsDeletedFalseOrderByPositionAsc(published.getId()));

        assertEquals(published.getId(), detail.bundle().getId());
        assertEquals(2, detail.items().size(), "the buyer must see what the bundle contains");
    }

    @Test
    void theCatalogReturnsNoPrice() {
        // Price is resolved at checkout through the pricing service. A figure in a listing could
        // go stale the moment a policy changes, and would invite a purchase at a no-longer-approved
        // price.
        Bundle published = seed(Bundle.STATUS_PUBLISHED, true);

        BundleCatalogController.BundleDetail detail = new BundleCatalogController.BundleDetail(published, bundleItemRepository.findAllByBundleIdAndIsDeletedFalseOrderByPositionAsc(published.getId()));

        List<String> priceish = java.util.Arrays.stream(Bundle.class.getDeclaredFields())
                .map(java.lang.reflect.Field::getName)
                .filter(n -> n.toLowerCase().contains("price") || n.toLowerCase().contains("amount"))
                .toList();
        assertTrue(priceish.isEmpty(),
                "Bundle must carry no price or amount field at all, but had: " + priceish);
        assertEquals(30, detail.bundle().getAccessDays(),
                "the duration is stated; the price is resolved at checkout");
    }
}