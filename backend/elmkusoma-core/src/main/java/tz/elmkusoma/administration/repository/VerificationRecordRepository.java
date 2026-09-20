package tz.elmkusoma.administration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tz.elmkusoma.administration.domain.VerificationRecord;

import java.util.List;
import java.util.UUID;

public interface VerificationRecordRepository extends JpaRepository<VerificationRecord, UUID> {
    List<VerificationRecord> findByStatusAndIsDeletedFalse(String status);
    List<VerificationRecord> findByEntityTypeAndEntityIdAndIsDeletedFalse(String entityType, UUID entityId);
    long countByStatusAndIsDeletedFalse(String status);
    long countByIsDeletedFalse();
}
