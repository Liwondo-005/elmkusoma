package tz.elmkusoma.parent.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.parent.domain.RefundRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefundRequestRepository extends JpaRepository<RefundRequest, UUID> {

    List<RefundRequest> findByPaymentIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID paymentId);

    List<RefundRequest> findByInstitutionIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID institutionId);

    List<RefundRequest> findByStatusAndIsDeletedFalseOrderByCreatedAtAsc(String status);

    Optional<RefundRequest> findFirstByPaymentIdAndStatusInAndIsDeletedFalse(
            UUID paymentId, List<String> statuses);

    boolean existsByPaymentIdAndStatusInAndIsDeletedFalse(UUID paymentId, List<String> statuses);

    long countByStatusAndIsDeletedFalse(String status);
}
