package tz.elmkusoma.parent.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.parent.domain.Payment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    List<Payment> findByParentIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID parentId);
    List<Payment> findByStudentIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID studentId);
    List<Payment> findByParentIdAndStatusAndIsDeletedFalse(UUID parentId, String status);
    List<Payment> findByStudentIdAndStatusAndIsDeletedFalse(UUID studentId, String status);
    long countByStudentIdAndStatusAndIsDeletedFalse(UUID studentId, String status);
    long countByParentIdAndStatusAndIsDeletedFalse(UUID parentId, String status);
    long countByIsDeletedFalse();
    Page<Payment> findByStatusAndIsDeletedFalse(String status, Pageable pageable);
    Page<Payment> findAllByIsDeletedFalse(Pageable pageable);
    boolean existsByProviderReferenceAndIdNotAndIsDeletedFalse(String providerReference, UUID id);

    /** Gateway (or manual) reference lookup - used to bind inbound callbacks to the local payment. */
    Optional<Payment> findByProviderReferenceAndIsDeletedFalse(String providerReference);

    /** Idempotent replay: the same purchase retried resolves to the payment already created. */
    Optional<Payment> findByIdempotencyKeyAndIsDeletedFalse(String idempotencyKey);

    List<Payment> findByStatusInAndIsDeletedFalse(List<String> statuses);

    List<Payment> findByPaidAtBetweenAndIsDeletedFalseOrderByPaidAtDesc(LocalDateTime from, LocalDateTime to);

    List<Payment> findByInstitutionIdAndPaidAtBetweenAndIsDeletedFalseOrderByPaidAtDesc(
            UUID institutionId, LocalDateTime from, LocalDateTime to);

    /**
     * Row-locked read for refund application.
     *
     * <p>Every path that moves money on a payment takes this lock before reading the refundable
     * balance, so two concurrent refunds cannot both observe the same remaining balance and both
     * pass the check. Same convention as {@code LiveClassRepository.findByIdForUpdate}, and the
     * payment row is the only lock taken, so the two refund paths cannot deadlock against each
     * other.</p>
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.id = :id")
    Optional<Payment> findByIdForUpdate(@Param("id") UUID id);
}
