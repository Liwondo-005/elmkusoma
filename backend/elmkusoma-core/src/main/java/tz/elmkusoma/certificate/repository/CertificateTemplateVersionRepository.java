package tz.elmkusoma.certificate.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.certificate.domain.CertificateTemplateVersion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CertificateTemplateVersionRepository extends JpaRepository<CertificateTemplateVersion, UUID> {

    List<CertificateTemplateVersion> findByTemplateIdAndIsDeletedFalseOrderByVersionDesc(UUID templateId);

    Optional<CertificateTemplateVersion> findByTemplateIdAndVersionAndIsDeletedFalse(UUID templateId, Integer version);

    long countByTemplateIdAndIsDeletedFalse(UUID templateId);
}
