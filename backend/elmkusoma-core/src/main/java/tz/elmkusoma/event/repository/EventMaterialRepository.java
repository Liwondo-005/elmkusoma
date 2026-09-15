package tz.elmkusoma.event.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.event.domain.EventMaterial;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventMaterialRepository extends JpaRepository<EventMaterial, UUID> {

    List<EventMaterial> findByEventIdAndIsDeletedFalseOrderBySortOrderAsc(UUID eventId);

    List<EventMaterial> findByEventIdAndMaterialTypeAndIsDeletedFalseOrderBySortOrderAsc(UUID eventId, String materialType);

    List<EventMaterial> findByEventIdAndIsPublicTrueAndIsDeletedFalseOrderBySortOrderAsc(UUID eventId);

    long countByEventIdAndIsDeletedFalse(UUID eventId);
}
