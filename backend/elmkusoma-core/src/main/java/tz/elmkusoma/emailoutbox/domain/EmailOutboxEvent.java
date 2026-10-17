package tz.elmkusoma.emailoutbox.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * A durable record that an email event must be sent.
 *
 * <p>This is deliberately <em>not</em> a log and <em>not</em> a delivery receipt. It
 * answers one question that a direct publish could not: <em>if the broker is down,
 * does the notification still exist?</em> The row is written in the same transaction
 * as the business record that caused it, so the two either both happen or neither
 * does.</p>
 *
 * <p>Status vocabulary, and the limit of what each state claims:</p>
 * <ul>
 *   <li>{@link #STATUS_PENDING} - written, not yet handed to the broker.</li>
 *   <li>{@link #STATUS_PUBLISHING} - claimed by a relay via a lease.</li>
 *   <li>{@link #STATUS_PUBLISHED} - the <b>broker accepted</b> the message. This says
 *       nothing about SMTP: rendering and delivery happen in elmkusoma-workers and can
 *       still fail there.</li>
 *   <li>{@link #STATUS_FAILED} - attempts exhausted. Terminal, and the reason is kept
 *       so an operator can act rather than guess.</li>
 * </ul>
 *
 * <p>Repeated requests are first-class. A subscriber who asks for two password resets
 * must get two emails, so identity comes from {@code id} and, where a caller wants a
 * retried request to collapse, from {@link #idempotencyKey} - never from recipient and
 * template alone.</p>
 */
@Entity
@Table(name = "email_outbox", indexes = {
        @Index(name = "idx_email_outbox_due", columnList = "next_attempt_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class EmailOutboxEvent extends BaseEntity {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PUBLISHING = "PUBLISHING";
    public static final String STATUS_PUBLISHED = "PUBLISHED";
    public static final String STATUS_FAILED = "FAILED";

    /** Ceiling on dispatch attempts. Past this the row is terminally failed. */
    public static final int DEFAULT_MAX_ATTEMPTS = 5;

    /** Longest error text retained, matching the column width. */
    private static final int MAX_ERROR_LENGTH = 500;

    @Column(name = "institution_id")
    private UUID institutionId;

    @Column(nullable = false, length = 320)
    private String recipient;

    @Column(nullable = false, length = 500)
    private String subject;

    @Column(name = "template_name", nullable = false, length = 100)
    private String templateName;

    /**
     * Template variables, stored as jsonb.
     *
     * <p>Persisted as JSON rather than a string map so a relay never has to re-parse
     * untrusted text, and so the structure survives a template gaining a variable.</p>
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "template_variables", columnDefinition = "jsonb")
    private Map<String, Object> templateVariables;

    @Column(nullable = false, length = 32)
    @Builder.Default
    private String status = STATUS_PENDING;

    @Column(nullable = false)
    @Builder.Default
    private int attempts = 0;

    @Column(name = "max_attempts", nullable = false)
    @Builder.Default
    private int maxAttempts = DEFAULT_MAX_ATTEMPTS;

    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "locked_by", length = 100)
    private String lockedBy;

    @Column(name = "last_error", length = 500)
    private String lastError;

    /**
     * Optional caller-supplied key that collapses a <em>retried</em> request onto the
     * same row. Left null by ordinary flows so that a genuinely repeated request still
     * produces its own notification.
     */
    @Column(name = "idempotency_key", length = 200)
    private String idempotencyKey;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    /** True while the row is eligible for dispatch and its lease has not lapsed. */
    public boolean isDispatchable(LocalDateTime now) {
        return STATUS_PENDING.equals(status)
                || (STATUS_PUBLISHING.equals(status) && leaseExpired(now));
    }

    /**
     * A relay holds the row only until its lease expires. If that relay dies, another
     * relay may reclaim the row - which is what stops a crash from stranding a
     * notification forever.
     */
    public boolean leaseExpired(LocalDateTime now) {
        return lockedUntil == null || !now.isBefore(lockedUntil);
    }

    public boolean isTerminallyFailed() {
        return STATUS_FAILED.equals(status);
    }

    /** Records a dispatch attempt, claiming the row for {@code leaseMinutes}. */
    public void claim(String worker, LocalDateTime now, int leaseMinutes) {
        this.status = STATUS_PUBLISHING;
        this.lockedBy = worker;
        this.lockedUntil = now.plusMinutes(leaseMinutes);
        this.attempts = this.attempts + 1;
    }

    /**
     * Marks the message as accepted by the broker.
     *
     * <p>Only the relay calls this, and only after publication met the configured
     * acceptance criteria. It records broker acceptance, not delivery.</p>
     */
    public void markPublished(LocalDateTime now) {
        this.status = STATUS_PUBLISHED;
        this.publishedAt = now;
        this.lastError = null;
        this.lockedBy = null;
        this.lockedUntil = null;
    }

    /**
     * Schedules another attempt, or gives up.
     *
     * <p>Backoff doubles per attempt from {@code baseBackoffSeconds} and is capped, so
     * a broker outage does not turn into a hot loop. Once {@link #maxAttempts} is
     * reached the row becomes {@link #STATUS_FAILED} and stops being picked up, which
     * is the observable terminal state operators alert on.</p>
     *
     * @return true when the event will be retried, false when it is now terminally failed
     */
    public boolean recordFailure(String error, LocalDateTime now, int baseBackoffSeconds) {
        this.lastError = truncate(error);
        this.lockedBy = null;
        this.lockedUntil = null;
        if (this.attempts >= this.maxAttempts) {
            this.status = STATUS_FAILED;
            return false;
        }
        this.status = STATUS_PENDING;
        long backoff = (long) baseBackoffSeconds * (1L << Math.max(0, this.attempts - 1));
        this.nextAttemptAt = now.plusSeconds(Math.min(backoff, 3600));
        return true;
    }

    private static String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= MAX_ERROR_LENGTH
                ? value
                : value.substring(0, MAX_ERROR_LENGTH);
    }
}