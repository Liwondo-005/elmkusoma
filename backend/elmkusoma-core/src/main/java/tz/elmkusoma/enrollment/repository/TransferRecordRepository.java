package tz.elmkusoma.enrollment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.enrollment.domain.TransferRecord;

import java.util.List;
import java.util.UUID;

@Repository
public interface TransferRecordRepository extends JpaRepository<TransferRecord, UUID> {

    List<TransferRecord> findByEnrollmentIdAndIsDeletedFalse(UUID enrollmentId);

    List<TransferRecord> findByInstitutionIdAndIsDeletedFalse(UUID institutionId);
}
