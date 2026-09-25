package tz.elmkusoma.certificate.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.certificate.domain.CertificateTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CertificateTemplateRepository extends JpaRepository<CertificateTemplate, UUID> {

    Optional<CertificateTemplate> findByIdAndIsDeletedFalse(UUID id);

    @Query("SELECT t FROM CertificateTemplate t WHERE t.institutionId = :institutionId AND t.isDeleted = false")
    List<CertificateTemplate> findAllByInstitutionId(@Param("institutionId") UUID institutionId);

    @Query("SELECT t FROM CertificateTemplate t WHERE t.institutionId = :institutionId AND t.templateType = :templateType AND t.isDeleted = false")
    List<CertificateTemplate> findByInstitutionIdAndTemplateType(@Param("institutionId") UUID institutionId,
                                                                  @Param("templateType") CertificateTemplate.TemplateType templateType);

    @Query("SELECT t FROM CertificateTemplate t WHERE t.institutionId = :institutionId AND t.isActive = true AND t.isDeleted = false")
    List<CertificateTemplate> findActiveByInstitutionId(@Param("institutionId") UUID institutionId);

    boolean existsByNameAndInstitutionIdAndIsDeletedFalse(String name, UUID institutionId);

    /** Platform-wide template search with optional filters (NULL = no restriction). */
    @Query("SELECT t FROM CertificateTemplate t WHERE t.isDeleted = false " +
            // :search must be '' (never NULL) - see the note in CertificateRepository.searchCertificates.
            "AND (:search = '' OR LOWER(t.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "     OR LOWER(t.description) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:type IS NULL OR t.templateType = :type) " +
            "AND (:institutionId IS NULL OR t.institutionId = :institutionId)")
    Page<CertificateTemplate> searchTemplates(@Param("search") String search,
                                              @Param("type") CertificateTemplate.TemplateType type,
                                              @Param("institutionId") UUID institutionId,
                                              Pageable pageable);
}
