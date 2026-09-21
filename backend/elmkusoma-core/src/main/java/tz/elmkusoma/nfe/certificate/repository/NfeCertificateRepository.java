package tz.elmkusoma.nfe.certificate.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.common.TenantRepository;
import tz.elmkusoma.nfe.certificate.domain.NfeCertificate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NfeCertificateRepository extends TenantRepository<NfeCertificate, UUID> {

    @Query("SELECT c FROM NfeCertificate c WHERE c.providerId = :providerId AND c.institutionId = :institutionId AND c.isDeleted = false")
    List<NfeCertificate> findByProviderId(@Param("providerId") UUID providerId, @Param("institutionId") UUID institutionId);

    @Query("SELECT c FROM NfeCertificate c WHERE c.learnerId = :learnerId AND c.institutionId = :institutionId AND c.isDeleted = false")
    List<NfeCertificate> findByLearnerId(@Param("learnerId") UUID learnerId, @Param("institutionId") UUID institutionId);

    @Query("SELECT c FROM NfeCertificate c WHERE c.verificationCode = :verificationCode AND c.institutionId = :institutionId AND c.isDeleted = false")
    Optional<NfeCertificate> findByVerificationCode(@Param("verificationCode") String verificationCode, @Param("institutionId") UUID institutionId);

    @Query("SELECT c FROM NfeCertificate c WHERE c.serialNumber = :serialNumber AND c.institutionId = :institutionId AND c.isDeleted = false")
    Optional<NfeCertificate> findBySerialNumber(@Param("serialNumber") String serialNumber, @Param("institutionId") UUID institutionId);

    @Query("SELECT c FROM NfeCertificate c WHERE c.id = :id AND c.institutionId = :institutionId AND c.isDeleted = false")
    Optional<NfeCertificate> findByIdAndInstitutionId(@Param("id") UUID id, @Param("institutionId") UUID institutionId);

    @Query("SELECT COUNT(c) FROM NfeCertificate c WHERE c.institutionId = :institutionId AND c.isDeleted = false")
    long countByInstitutionId(@Param("institutionId") UUID institutionId);
}
