package tz.elmkusoma.workers.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.workers.domain.WorkerNotification;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkerNotificationRepository extends JpaRepository<WorkerNotification, UUID> {

    // Audit B-06: keys are UUID to match core's users/institutions (V002 widened the columns).
    List<WorkerNotification> findByUserIdAndIsReadFalse(UUID userId);

    List<WorkerNotification> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<WorkerNotification> findByInstitutionIdOrderByCreatedAtDesc(UUID institutionId);

    long countByUserIdAndIsReadFalse(UUID userId);

    /** Platform-wide unread total, replacing the hardcoded user id 1 in the digest scheduler. */
    long countByIsReadFalse();
}