package tz.elmkusoma.workers.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.workers.domain.WorkerNotification;

import java.util.List;

@Repository
public interface WorkerNotificationRepository extends JpaRepository<WorkerNotification, Long> {

    List<WorkerNotification> findByUserIdAndIsReadFalse(Long userId);

    List<WorkerNotification> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<WorkerNotification> findByInstitutionIdOrderByCreatedAtDesc(Long institutionId);

    long countByUserIdAndIsReadFalse(Long userId);
}
