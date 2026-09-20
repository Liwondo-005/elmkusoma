package tz.elmkusoma.administration.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import tz.elmkusoma.administration.domain.PlatformIncident;

import java.util.UUID;

public interface PlatformIncidentRepository extends JpaRepository<PlatformIncident, UUID> {
    Page<PlatformIncident> findByIsDeletedFalseOrderByDetectedAtDesc(Pageable pageable);
    Page<PlatformIncident> findByStatusAndIsDeletedFalseOrderByDetectedAtDesc(String status, Pageable pageable);
    Page<PlatformIncident> findBySeverityAndIsDeletedFalseOrderByDetectedAtDesc(String severity, Pageable pageable);
    long countByStatusAndIsDeletedFalse(String status);
    long countByIsDeletedFalse();
}
