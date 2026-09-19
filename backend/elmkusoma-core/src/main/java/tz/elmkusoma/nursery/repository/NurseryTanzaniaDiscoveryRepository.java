package tz.elmkusoma.nursery.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.nursery.domain.NurseryTanzaniaDiscovery;

import java.util.List;
import java.util.UUID;

@Repository
public interface NurseryTanzaniaDiscoveryRepository extends JpaRepository<NurseryTanzaniaDiscovery, UUID> {
    List<NurseryTanzaniaDiscovery> findByIsPublishedAndIsDeletedFalse(Boolean isPublished);
    List<NurseryTanzaniaDiscovery> findByCategoryAndIsDeletedFalse(NurseryTanzaniaDiscovery.DiscoveryCategory category);
    List<NurseryTanzaniaDiscovery> findByClassGroupIdAndIsDeletedFalse(UUID classGroupId);
}
