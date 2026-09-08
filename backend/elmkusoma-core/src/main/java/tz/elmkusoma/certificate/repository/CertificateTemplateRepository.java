package tz.elmkusoma.certificate.repository;

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
}
