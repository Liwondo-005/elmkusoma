package tz.elmkusoma.learner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.learner.domain.LearnerNotification;

import java.util.List;
import java.util.UUID;

@Repository
public interface LearnerNotificationRepository extends JpaRepository<LearnerNotification, UUID> {

    List<LearnerNotification> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID userId);

    /**
     * One notification owned by exactly this user.
     *
     * <p>Ownership travels in the query rather than being checked afterwards, so a caller cannot
     * reach another user's row even by guessing an id: a row belonging to somebody else simply
     * does not exist as far as this method is concerned, and the caller is told the same thing it
     * would be told for an id that was never issued. That symmetry is what keeps the endpoint from
     * becoming an existence oracle.
     */
    java.util.Optional<LearnerNotification> findByIdAndUserIdAndIsDeletedFalse(UUID id, UUID userId);

    long countByUserIdAndIsReadFalseAndIsDeletedFalse(UUID userId);

    List<LearnerNotification> findByUserIdAndIsReadFalseAndIsDeletedFalseOrderByCreatedAtDesc(UUID userId);

    /** Idempotency guard for event reminders (§18/19): one notification per user/event/offset. */
    boolean existsByUserIdAndTargetIdAndNotificationTypeAndIsDeletedFalse(
            UUID userId, UUID targetId, String notificationType);

    List<LearnerNotification> findByTargetIdAndNotificationTypeAndIsDeletedFalse(
            UUID targetId, String notificationType);
}
