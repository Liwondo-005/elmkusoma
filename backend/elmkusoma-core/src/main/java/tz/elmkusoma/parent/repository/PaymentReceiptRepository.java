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

    List<PaymentReceipt> findByStudentIdAndIsDeletedFalseOrderByPaidAtDesc(UUID studentId);

    List<PaymentReceipt> findByInstitutionIdAndIsDeletedFalseOrderByPaidAtDesc(UUID institutionId);

    boolean existsByPaymentIdAndIsDeletedFalse(UUID paymentId);
}
