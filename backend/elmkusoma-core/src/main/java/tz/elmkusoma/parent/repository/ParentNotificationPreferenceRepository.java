package tz.elmkusoma.parent.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.parent.domain.ParentNotificationPreference;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ParentNotificationPreferenceRepository extends JpaRepository<ParentNotificationPreference, UUID> {

    @Query("SELECT p FROM ParentNotificationPreference p WHERE p.parentId = :parentId AND p.isDeleted = false")
    Optional<ParentNotificationPreference> findByParentId(@Param("parentId") UUID parentId);
}
