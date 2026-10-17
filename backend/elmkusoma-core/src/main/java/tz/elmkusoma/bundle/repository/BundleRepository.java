package tz.elmkusoma.bundle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.bundle.domain.Bundle;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BundleRepository extends JpaRepository<Bundle, UUID> {

    Optional<Bundle> findByServiceTypeAndIsDeletedFalse(String serviceType);

    List<Bundle> findAllByInstitutionIdAndIsDeletedFalseOrderByNameAsc(UUID institutionId);

    /** Only what a learner may actually buy. Suspension is a sale rule, not a display filter. */
    List<Bundle> findAllByStatusAndIsDeletedFalseOrderByNameAsc(String status);
}