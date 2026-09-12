package tz.elmkusoma.learner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.learner.domain.LearnerNotification;

import java.util.List;
import java.util.UUID;

@Repository
public interface LearnerNotificationRepository extends JpaRepository<LearnerNotification, UUID> {

    List<LearnerNotification> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID userId);

    long countByUserIdAndIsReadFalseAndIsDeletedFalse(UUID userId);

    List<LearnerNotification> findByUserIdAndIsReadFalseAndIsDeletedFalseOrderByCreatedAtDesc(UUID userId);
}
