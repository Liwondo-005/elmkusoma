package tz.elmkusoma.certificate.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.certificate.domain.CertificateSignatory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CertificateSignatoryRepository extends JpaRepository<CertificateSignatory, UUID> {

    Optional<CertificateSignatory> findByIdAndIsDeletedFalse(UUID id);

    @Query("SELECT s FROM CertificateSignatory s WHERE s.isDeleted = false " +
            // :search must be '' (never NULL) - see the note in CertificateRepository.searchCertificates.
            "AND (:search = '' OR LOWER(s.fullName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "     OR LOWER(s.organization) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "     OR LOWER(s.positionTitle) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:status IS NULL OR s.status = :status) " +
            "AND (:institutionId IS NULL OR s.institutionId = :institutionId OR s.institutionId IS NULL) " +
            "ORDER BY s.fullName")
    Page<CertificateSignatory> search(@Param("search") String search,
                                      @Param("status") CertificateSignatory.SignatoryStatus status,
                                      @Param("institutionId") UUID institutionId,
                                      Pageable pageable);
}
