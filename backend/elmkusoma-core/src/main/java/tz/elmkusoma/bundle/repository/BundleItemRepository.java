package tz.elmkusoma.bundle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.bundle.domain.BundleItem;

import java.util.List;
import java.util.UUID;

@Repository
public interface BundleItemRepository extends JpaRepository<BundleItem, UUID> {

    /** Ordered so a bundle always presents the same way. */
    List<BundleItem> findAllByBundleIdAndIsDeletedFalseOrderByPositionAsc(UUID bundleId);

    List<BundleItem> findAllByBundleIdInAndIsDeletedFalse(List<UUID> bundleIds);
}