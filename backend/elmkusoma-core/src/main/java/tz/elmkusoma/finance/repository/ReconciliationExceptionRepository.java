package tz.elmkusoma.finance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.finance.domain.ReconciliationException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReconciliationExceptionRepository extends JpaRepository<ReconciliationException, UUID> {

    List<ReconciliationException> findByStatusAndIsDeletedFalseOrderByDiscoveredAtDesc(String status);

    List<ReconciliationException> findByInstitutionIdAndIsDeletedFalseOrderByDiscoveredAtDesc(UUID institutionId);

    Optional<ReconciliationException> findFirstByExceptionTypeAndReferenceAndStatusAndIsDeletedFalse(
            String exceptionType, String reference, String status);

    boolean existsByExceptionTypeAndReferenceAndStatusAndIsDeletedFalse(
            String exceptionType, String reference, String status);

    long countByStatusAndIsDeletedFalse(String status);
}
