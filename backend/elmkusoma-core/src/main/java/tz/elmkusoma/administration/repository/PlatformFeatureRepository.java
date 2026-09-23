package tz.elmkusoma.administration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.administration.domain.PlatformFeature;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlatformFeatureRepository extends JpaRepository<PlatformFeature, UUID> {
    List<PlatformFeature> findByIsDeletedFalse();
    Optional<PlatformFeature> findByFeatureKeyAndIsDeletedFalse(String featureKey);
}
