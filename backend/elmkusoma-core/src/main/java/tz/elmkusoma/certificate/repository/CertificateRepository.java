package tz.elmkusoma.certificate.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Query("SELECT c FROM Certificate c WHERE c.studentId = :studentId AND c.status = 'ISSUED' AND c.isDeleted = false ORDER BY c.issueDate DESC")
    List<Certificate> findIssuedByStudentId(@Param("studentId") UUID studentId);

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

    long countByIsDeletedFalse();

    Page<Certificate> findAllByIsDeletedFalse(Pageable pageable);

    Page<Certificate> findByStudentIdAndIsDeletedFalse(UUID studentId, Pageable pageable);

    /**
     * Tenant-scoped lookup. Callers must use this rather than the studentId-only variant: a
     * certificate belongs to one institution, and a query that filters on student alone will
     * happily return another school's rows for a student id from a different tenant.
     */
    Page<Certificate> findByStudentIdAndInstitutionIdAndIsDeletedFalse(UUID studentId,
                                                                       UUID institutionId,
                                                                       Pageable pageable);

    Page<Certificate> findByInstitutionIdAndIsDeletedFalse(UUID institutionId, Pageable pageable);

    long countByInstitutionIdAndStatusAndIsDeletedFalse(UUID institutionId, Certificate.CertificateStatus status);

    /**
     * Platform-wide certificate search with optional filters. Every filter is optional
     * (NULL = no restriction) so a plain call with no filters returns the same
     * unfiltered page as before.
     */
    @Query("SELECT c FROM Certificate c WHERE c.isDeleted = false " +
            // :search is always bound as '' (never NULL): Hibernate cannot infer the type of an
            // untyped NULL used only in LIKE/CONCAT, so a null bind is sent as JAVA_OBJECT ->
            // pgjdbc declares the parameter bytea -> Postgres rejects lower(bytea) (error 42883).
            "AND (:search = '' OR LOWER(c.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "     OR LOWER(c.serialNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "     OR LOWER(c.studentName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "     OR LOWER(c.certificateNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "     OR LOWER(c.courseOrProgramme) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:status IS NULL OR c.status = :status) " +
            "AND (:type IS NULL OR c.certificateType = :type) " +
            "AND (:institutionId IS NULL OR c.institutionId = :institutionId) " +
            // issue_date is NOT NULL and the service always binds a wide sentinel range for
            // omitted date filters: Postgres cannot infer the type of a parameter that only
            // appears in an IS NULL check (error 42P18), which broke the whole list at runtime.
            "AND c.issueDate >= :fromDate AND c.issueDate <= :toDate")
    Page<Certificate> searchCertificates(@Param("search") String search,
                                          @Param("status") Certificate.CertificateStatus status,
                                          @Param("type") Certificate.CertificateType type,
                                          @Param("institutionId") UUID institutionId,
                                          @Param("fromDate") java.time.LocalDateTime fromDate,
                                          @Param("toDate") java.time.LocalDateTime toDate,
                                          Pageable pageable);

    long countByStatusAndIsDeletedFalse(Certificate.CertificateStatus status);

    long countByCertificateTypeAndIsDeletedFalse(Certificate.CertificateType type);

    long countByTemplateIdAndIsDeletedFalse(UUID templateId);

    /**
     * A live certificate already awarded to this student for the same template, type and title.
     *
     * <p>Used to refuse a second issuance of an award the student already holds. Revoked
     * certificates are excluded on purpose: if an award is withdrawn, re-issuing it is the correct
     * remedy and must not be blocked by the record of the mistake. Soft-deleted rows are excluded
     * for the same reason.</p>
     *
     * <p>This is an application-level check for a clear message. It is not sufficient on its own,
     * because two concurrent issuances can both read "none exists" before either writes; V162 adds
     * the partial unique index that actually makes the check safe under concurrency.</p>
     */
    @Query("SELECT c FROM Certificate c WHERE c.studentId = :studentId "
            + "AND c.templateId = :templateId "
            + "AND c.certificateType = :certificateType "
            + "AND c.title = :title "
            + "AND c.isDeleted = false "
            + "AND c.status <> tz.elmkusoma.certificate.domain.Certificate$CertificateStatus.REVOKED")
    Optional<Certificate> findLiveAward(@Param("studentId") UUID studentId,
                                        @Param("templateId") UUID templateId,
                                        @Param("certificateType") Certificate.CertificateType certificateType,
                                        @Param("title") String title);

    /**
     * Loads many certificates at once, skipping soft-deleted rows.
     *
     * <p>Used by the approval queue, which renders a list of pending decisions. Each decision needs
     * the certificate's serial, title and recipient to be reviewable at all, and resolving them one
     * at a time would make a single page view into one query per row.</p>
     */
    List<Certificate> findByIdInAndIsDeletedFalse(List<UUID> ids);
}
