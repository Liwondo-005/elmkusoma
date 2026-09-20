package tz.elmkusoma.administration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tz.elmkusoma.administration.domain.PlatformConfigEntry;

import java.util.Optional;
import java.util.UUID;

public interface PlatformConfigRepository extends JpaRepository<PlatformConfigEntry, UUID> {
    Optional<PlatformConfigEntry> findByConfigKeyAndIsDeletedFalse(String configKey);
    java.util.List<PlatformConfigEntry> findByCategoryAndIsDeletedFalse(String category);
    java.util.List<PlatformConfigEntry> findByIsDeletedFalse();
}
