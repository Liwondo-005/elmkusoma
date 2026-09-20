package tz.elmkusoma.administration.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import tz.elmkusoma.administration.domain.PlatformService;

import java.util.Optional;
import java.util.UUID;

public interface PlatformServiceRepository extends JpaRepository<PlatformService, UUID> {
    Page<PlatformService> findByIsDeletedFalse(Pageable pageable);
    Page<PlatformService> findByCategoryAndIsDeletedFalse(String category, Pageable pageable);
    Optional<PlatformService> findByCodeAndIsDeletedFalse(String code);
    long countByIsDeletedFalse();
    long countByIsActiveAndIsDeletedFalse(boolean isActive);
}
