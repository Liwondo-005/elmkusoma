package tz.elmkusoma.certificate.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.certificate.domain.CertificateTemplateSignatory;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface CertificateTemplateSignatoryRepository extends JpaRepository<CertificateTemplateSignatory, UUID> {

    @Query("SELECT l FROM CertificateTemplateSignatory l WHERE l.templateId = :templateId AND l.isDeleted = false ORDER BY l.displayOrder")
    List<CertificateTemplateSignatory> findByTemplateId(@Param("templateId") UUID templateId);

    @Query("SELECT l FROM CertificateTemplateSignatory l WHERE l.templateId IN :templateIds AND l.isDeleted = false ORDER BY l.displayOrder")
    List<CertificateTemplateSignatory> findByTemplateIdIn(@Param("templateIds") Collection<UUID> templateIds);

    @Query("SELECT l FROM CertificateTemplateSignatory l WHERE l.signatoryId = :signatoryId AND l.isDeleted = false")
    List<CertificateTemplateSignatory> findBySignatoryId(@Param("signatoryId") UUID signatoryId);

    /**
     * All link rows for a template, including soft-deleted ones. Re-linking must reuse the
     * existing row because UNIQUE (template_id, signatory_id) applies regardless of
     * is_deleted (see replaceTemplateSignatories).
     */
    @Query("SELECT l FROM CertificateTemplateSignatory l WHERE l.templateId = :templateId")
    List<CertificateTemplateSignatory> findAllByTemplateIdIncludingDeleted(@Param("templateId") UUID templateId);

    long countByTemplateIdAndIsDeletedFalse(UUID templateId);
}
