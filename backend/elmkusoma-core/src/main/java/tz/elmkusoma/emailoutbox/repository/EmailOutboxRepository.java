package tz.elmkusoma.emailoutbox.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.emailoutbox.domain.EmailOutboxEvent;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Dispatch-side access to the email outbox.
 *
 * <p>Two rules shape the queries here.</p>
 *
 * <p><b>Claiming is a row lock, not a flag check.</b> Two relays starting at once must
 * not both pick up the same notification. {@link #findDueForUpdate} takes a
 * {@code PESSIMISTIC_WRITE} lock on the batch it returns, so the second relay blocks
 * and then re-reads the already-advanced rows.</p>
 *
 * <p><b>A row is "due" if it was never claimed, or its lease lapsed.</b> Including
 * lapsed {@code PUBLISHING} rows is what makes the system self-healing: a relay that
 * is killed mid-publish leaves a lease that simply expires, and the next poll reclaims
 * it. Without that, one crash would strand a notification permanently.</p>
 */
@Repository
public interface EmailOutboxRepository extends JpaRepository<EmailOutboxEvent, UUID> {

    /**
     * A batch of events that are due for dispatch, locked for update.
     *
     * <p>Must be called inside a transaction. The lock is held only for the duration
     * of the surrounding transaction, which is why the relay copies the payloads it
     * needs before doing any network work.</p>
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM EmailOutboxEvent e WHERE e.isDeleted = false "
            + "AND e.nextAttemptAt <= :now "
            + "AND (e.status = 'PENDING' OR (e.status = 'PUBLISHING' AND "
            + "     (e.lockedUntil IS NULL OR e.lockedUntil <= :now))) "
            + "ORDER BY e.nextAttemptAt ASC")
    List<EmailOutboxEvent> findDueForUpdate(@Param("now") LocalDateTime now, Pageable pageable);

    Optional<EmailOutboxEvent> findByIdempotencyKeyAndIsDeletedFalse(String idempotencyKey);

    long countByStatusAndIsDeletedFalse(String status);
}