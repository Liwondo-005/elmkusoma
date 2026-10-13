package tz.elmkusoma.shared.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A claimed idempotency key for a communication write.
 *
 * <p>One row per (actor, operation, key). The uniqueness is the point: an application-level
 * "have I seen this?" check cannot stop two simultaneous retries, but a unique constraint can,
 * so the guarantee lives in the database rather than in a race-prone read-then-write.</p>
 *
 * <p>{@code requestHash} pins the payload the key was first used with, so replaying a key with
 * different content is refused instead of quietly returning a result computed from something
 * else. Only the hash is stored — never the body, which may hold personal data and has no
 * business being duplicated into a retry ledger.</p>
 *
 * <p>A row with {@code completedAt == null} is a claim still in flight, or one whose request
 * crashed before committing. Either way it is not a finished result and must not be replayed
 * as one.</p>
 *
 * <p>The unique constraint is declared here and not only in the Flyway migration, because the
 * test schema is built by Hibernate from these annotations (Flyway is disabled under test).
 * The {@code ON CONFLICT} claim targets that constraint by name, so if the two definitions
 * ever drifted the claim would fail at runtime rather than at deploy time.</p>
 */
@Entity
@Table(name = "communication_idempotency_keys",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_comm_idem_actor_key",
                columnNames = {"actor_key", "idempotency_key"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CommunicationIdempotencyKey extends BaseEntity {

    /** The authenticated user's id, or {@code ip:<address>} for an anonymous submission. */
    @Column(name = "actor_key", nullable = false, length = 64)
    private String actorKey;

    /** TICKET_CREATE | TICKET_MESSAGE | ENQUIRY_CREATE | ENQUIRY_REPLY */
    @Column(nullable = false, length = 32)
    private String operation;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    /** SHA-256 of the canonical payload. Deliberately not the payload. */
    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "resource_id")
    private UUID resourceId;

    @Column(name = "resource_text", length = 64)
    private String resourceText;

    /** Null while in flight or after a crash; set once the work has genuinely committed. */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    public boolean isCompleted() {
        return completedAt != null;
    }
}
