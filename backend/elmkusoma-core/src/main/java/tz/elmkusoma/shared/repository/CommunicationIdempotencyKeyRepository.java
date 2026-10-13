package tz.elmkusoma.shared.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.shared.domain.CommunicationIdempotencyKey;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommunicationIdempotencyKeyRepository
        extends JpaRepository<CommunicationIdempotencyKey, UUID> {

    /**
     * Claims a key in a single statement.
     *
     * <p>{@code ON CONFLICT DO NOTHING} is what makes this safe under concurrency: two
     * simultaneous retries race to insert, the database lets exactly one through, and the other
     * sees a zero row-count rather than an exception that would poison its transaction. A
     * read-then-write check cannot offer that — both requests would read "not claimed" and both
     * would proceed.</p>
     *
     * <p>The conflict target is {@code (actor_key, idempotency_key)} without {@code operation},
     * so a key reused for a *different* operation collides too rather than silently opening a
     * second row; that collision is what lets the service refuse it as a client bug.</p>
     *
     * @return 1 when this caller now owns the key, 0 when it was already taken.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            INSERT INTO communication_idempotency_keys
                (id, actor_key, operation, idempotency_key, request_hash,
                 created_at, expires_at, is_deleted)
            VALUES (:id, :actorKey, :operation, :idempotencyKey, :requestHash,
                    :now, :expiresAt, false)
            ON CONFLICT (actor_key, idempotency_key) DO NOTHING
            """, nativeQuery = true)
    int tryClaim(@Param("id") UUID id,
                 @Param("actorKey") String actorKey,
                 @Param("operation") String operation,
                 @Param("idempotencyKey") String idempotencyKey,
                 @Param("requestHash") String requestHash,
                 @Param("now") LocalDateTime now,
                 @Param("expiresAt") LocalDateTime expiresAt);

    /**
     * Loads by identity alone, without {@code operation}.
     *
     * <p>Operation is then compared by the caller, not folded into the query: a key reused for a
     * different operation must still be *found* so it can be refused, whereas filtering on the
     * incoming operation here would miss the existing row and let the reuse through as new.</p>
     */
    Optional<CommunicationIdempotencyKey> findByActorKeyAndIdempotencyKeyAndIsDeletedFalse(
            String actorKey, String idempotencyKey);

    /**
     * Records what the first successful run produced, so a replay returns the original result
     * instead of an empty acknowledgement.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE CommunicationIdempotencyKey k
               SET k.resourceId = :resourceId,
                   k.resourceText = :resourceText,
                   k.completedAt = :completedAt
             WHERE k.id = :id
            """)
    int complete(@Param("id") UUID id,
                 @Param("resourceId") UUID resourceId,
                 @Param("resourceText") String resourceText,
                 @Param("completedAt") LocalDateTime completedAt);

    /**
     * Reclaims a key left behind by a request that died before it could release it.
     *
     * <p>A claim with no result older than the stale threshold cannot still be in flight, so
     * it is removed and the retry is allowed to proceed rather than being blocked forever by a
     * request that will never finish.</p>
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            DELETE FROM communication_idempotency_keys
             WHERE actor_key = :actorKey
               AND idempotency_key = :idempotencyKey
               AND completed_at IS NULL
               AND created_at < :staleBefore
            """, nativeQuery = true)
    int abandonStale(@Param("actorKey") String actorKey,
                     @Param("idempotencyKey") String idempotencyKey,
                     @Param("staleBefore") LocalDateTime staleBefore);

    /** Retention sweep: finished keys past their expiry window. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            DELETE FROM communication_idempotency_keys
             WHERE completed_at IS NOT NULL
               AND expires_at < :now
            """, nativeQuery = true)
    int deleteExpiredCompleted(@Param("now") LocalDateTime now);

    /** Retention sweep: abandoned claims are dead weight once past a generous grace period. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            DELETE FROM communication_idempotency_keys
             WHERE completed_at IS NULL
               AND created_at < :now
            """, nativeQuery = true)
    int deleteExpiredUncompleted(@Param("now") LocalDateTime now);
}
