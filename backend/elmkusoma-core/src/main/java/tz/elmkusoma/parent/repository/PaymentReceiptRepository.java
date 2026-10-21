package tz.elmkusoma.parent.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.parent.domain.PaymentReceipt;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentReceiptRepository extends JpaRepository<PaymentReceipt, UUID> {

    Optional<PaymentReceipt> findByPaymentIdAndIsDeletedFalse(UUID paymentId);

    Optional<PaymentReceipt> findByReceiptNumberAndIsDeletedFalse(String receiptNumber);

    Optional<PaymentReceipt> findByParentIdAndPaymentIdAndIsDeletedFalse(UUID parentId, UUID paymentId);

    List<PaymentReceipt> findByParentIdAndIsDeletedFalseOrderByPaidAtDesc(UUID parentId);

    /**
     * Receipts owned by any of these identifiers.
     *
     * <p>Needed because {@code parent_id} carries two different things depending on how the money
     * was taken: the parent's <em>user</em> id for a payment raised from the parent dashboard, and
     * the parent <em>record's</em> id for an event ticket. A list filtered on the user id alone
     * therefore silently omitted every ticket the parent had bought, which is worse than a refusal
     * because it looks like a complete history.
     */
    List<PaymentReceipt> findByParentIdInAndIsDeletedFalseOrderByPaidAtDesc(
            java.util.Collection<UUID> parentIds);

    List<PaymentReceipt> findByStudentIdAndIsDeletedFalseOrderByPaidAtDesc(UUID studentId);

    List<PaymentReceipt> findByInstitutionIdAndIsDeletedFalseOrderByPaidAtDesc(UUID institutionId);

    boolean existsByPaymentIdAndIsDeletedFalse(UUID paymentId);
}
