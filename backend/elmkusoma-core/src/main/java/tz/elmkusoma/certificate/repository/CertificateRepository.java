package tz.elmkusoma.certificate.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.certificate.domain.Certificate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, UUID> {

    Optional<Certificate> findByIdAndIsDeletedFalse(UUID id);

    Optional<Certificate> findBySerialNumberAndIsDeletedFalse(String serialNumber);

    Optional<Certificate> findByVerificationCodeAndIsDeletedFalse(String verificationCode);

    @Query("SELECT c FROM Certificate c WHERE c.institutionId = :institutionId AND c.isDeleted = false ORDER BY c.createdAt DESC")
    List<Certificate> findAllByInstitutionId(@Param("institutionId") UUID institutionId);

    @Query("SELECT c FROM Certificate c WHERE c.studentId = :studentId AND c.isDeleted = false ORDER BY c.createdAt DESC")
    List<Certificate> findAllByStudentId(@Param("studentId") UUID studentId);

    @Query("SELECT c FROM Certificate c WHERE c.institutionId = :institutionId AND c.status = :status AND c.isDeleted = false")
    List<Certificate> findByInstitutionIdAndStatus(@Param("institutionId") UUID institutionId,
                                                    @Param("status") Certificate.CertificateStatus status);

    @Query("SELECT c FROM Certificate c WHERE c.issuedBy = :issuedBy AND c.isDeleted = false")
    List<Certificate> findAllByIssuedBy(@Param("issuedBy") UUID issuedBy);

    @Query("SELECT COUNT(c) FROM Certificate c WHERE c.institutionId = :institutionId AND c.isDeleted = false")
    long countByInstitutionId(@Param("institutionId") UUID institutionId);

    @Query("SELECT COUNT(c) FROM Certificate c WHERE c.institutionId = :institutionId AND c.status = :status AND c.isDeleted = false")
    long countByInstitutionIdAndStatus(@Param("institutionId") UUID institutionId,
                                        @Param("status") Certificate.CertificateStatus status);
}
