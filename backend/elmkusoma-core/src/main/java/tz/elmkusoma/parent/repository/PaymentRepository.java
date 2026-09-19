package tz.elmkusoma.parent.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.parent.domain.Payment;

import java.util.List;
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
}
