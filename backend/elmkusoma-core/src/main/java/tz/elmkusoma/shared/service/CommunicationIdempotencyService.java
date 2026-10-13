package tz.elmkusoma.shared.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.shared.domain.CommunicationIdempotencyKey;
import tz.elmkusoma.shared.repository.CommunicationIdempotencyKeyRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

/**
 * Safe-retry for communication writes.
 *
 * <p>The failure this exists to stop is ordinary, not exotic: a parent taps "submit" twice, a
 * mobile connection drops after the server has committed but before the response lands, a proxy
 * times out and the client retries. Each of those used to produce a second ticket or a second
 * message. None of them is a bug in the client — they are the normal condition of a network —
 * so the server has to absorb them.</p>
 *
 * <p>The guarantee is a database unique constraint, not a check-then-act. Two retries arriving
 * together both pass any SELECT, so an application-level "have I seen this?" question cannot
 * settle it; exactly one INSERT winning can.</p>
 *
 * <p>Reused with different content, a key is a client bug, not a replay, and is refused. A hash
 * of the payload is enough to tell the two apart, and a hash is deliberate: the ledger must not
 * become a second copy of messages and enquiries that may carry personal data.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommunicationIdempotencyService {

    public static final String OP_TICKET_CREATE = "TICKET_CREATE";
    public static final String OP_TICKET_MESSAGE = "TICKET_MESSAGE";
    public static final String OP_ENQUIRY_CREATE = "ENQUIRY_CREATE";
    public static final String OP_ENQUIRY_REPLY = "ENQUIRY_REPLY";

    /** How long a key is honoured. Long enough to cover a client's retry horizon, not forever. */
    private static final int RETENTION_DAYS = 7;

    /**
     * An unfinished claim older than this cannot still be in flight, so it is treated as the
     * wreckage of a request that died and is cleared rather than honoured.
     */
    private static final int STALE_CLAIM_MINUTES = 5;

    private final CommunicationIdempotencyKeyRepository repository;

    /** Outcome of an idempotent execution. */
    public record Outcome<T>(T result, boolean replayed) {
        public static <T> Outcome<T> fresh(T result) {
            return new Outcome<>(result, false);
        }

        public static <T> Outcome<T> replay(T result) {
            return new Outcome<>(result, true);
        }
    }

    /**
     * Raised when a key is reused for content other than the content it was first used with.
     *
     * <p>Extends {@link IllegalStateException} so the existing exception handler answers 409
     * Conflict: a replayed key with different content, or one still in flight, is exactly that.
     * A 500 would tell the client its retry was a server fault when it was neither.</p>
     */
    public static class ConflictingReuseException extends IllegalStateException {
        public ConflictingReuseException(String message) {
            super(message);
        }
    }

    /**
     * The stored result of a first run, for honouring a replay.
     *
     * <p>{@code resourceId} is the created row's id; {@code resourceText} carries the one extra
     * value a replay has to reproduce — the human-quotable enquiry reference, which is generated
     * per creation and would otherwise differ between the original response and the retry.</p>
     */
    public record PriorResult(UUID resourceId, String resourceText) {
    }

    /**
     * Runs {@code work} at most once per (actor, operation, key).
     *
     * <p>On a replay the stored prior result is returned and {@code work} is not invoked, so a
     * duplicate submission cannot produce a second message, a second ticket, or a second
     * notification event downstream.</p>
     *
     * @param idOf    extracts the row id from a freshly created result, so a replay can be
     *                answered from the ledger rather than by re-running the work
     * @param priorFetcher rebuilds the caller's result type from that stored id
     * @throws ConflictingReuseException if the key was first used with a different payload
     */
    // REQUIRED, not MANDATORY: the claim, the work and the completion must share one
    // transaction, but callers differ — service methods are already transactional while the
    // admin controller is not, and MANDATORY made that path fail outright. REQUIRED joins an
    // existing transaction when there is one and opens one when there is not, so either way the
    // three steps commit together and a rollback discards the claim with the work.
    @Transactional(propagation = Propagation.REQUIRED)
    public <T> Outcome<T> execute(String actorKey, String operation, String idempotencyKey,
                                  String requestHash, PriorFetcher<T> priorFetcher,
                                  java.util.function.Supplier<T> work,
                                  java.util.function.Function<T, UUID> idOf) {

        UUID claimId = claim(actorKey, operation, idempotencyKey, requestHash);
        if (claimId == null) {
            // Someone else holds this key. Decide between replay and conflict from the row.
            return honourExisting(actorKey, operation, idempotencyKey, requestHash, priorFetcher);
        }

        // No cleanup on failure: the claim and the work share this transaction, so an exception
        // rolls both back and the key is free again for the client's honest retry. Releasing it
        // explicitly here would run a DELETE against a transaction already marked rollback-only.
        T result = work.get();
        complete(claimId, idOf.apply(result), null);
        return Outcome.fresh(result);
    }

    /** Rebuilds the caller's result type from what the first run stored. */
    @FunctionalInterface
    public interface PriorFetcher<T> {
        T fromPrior(PriorResult prior);
    }

    /**
     * Attempts to take the key.
     *
     * <p>Not {@code @Transactional} itself: as a private method Spring cannot proxy it, and it
     * does not need to be — the surrounding {@link #execute} already holds the transaction the
     * claim, the work and the completion share.</p>
     *
     * @return the claim id on success, or {@code null} if the key was already held
     */
    private UUID claim(String actorKey, String operation, String idempotencyKey, String requestHash) {
        // Clear a crashed request's leftover claim first, so a retry is not blocked forever by
        // work that will never finish.
        repository.abandonStale(actorKey, idempotencyKey,
                LocalDateTime.now().minusMinutes(STALE_CLAIM_MINUTES));

        UUID candidate = UUID.randomUUID();
        int inserted;
        try {
            inserted = repository.tryClaim(candidate, actorKey, operation, idempotencyKey,
                    requestHash, LocalDateTime.now(), LocalDateTime.now().plusDays(RETENTION_DAYS));
        } catch (DataIntegrityViolationException race) {
            // Two retries landed on the same INSERT. The constraint settled it; this one lost.
            inserted = 0;
        }
        return inserted == 1 ? candidate : null;
    }

    /** Decides between replay and conflict for a key this caller does not hold. */
    @SuppressWarnings("unchecked")
    private <T> Outcome<T> honourExisting(String actorKey, String operation, String idempotencyKey,
                                          String requestHash, PriorFetcher<T> priorFetcher) {
        Optional<CommunicationIdempotencyKey> held =
                repository.findByActorKeyAndIdempotencyKeyAndIsDeletedFalse(actorKey, idempotencyKey);

        if (held.isEmpty()) {
            // Released between our failed claim and this read. Treat it as contention rather
            // than inventing an answer; the client's retry semantics are unaffected.
            throw new ConflictingReuseException(
                    "This idempotency key is being processed. Retry the request.");
        }

        CommunicationIdempotencyKey key = held.get();
        if (!operation.equals(key.getOperation())) {
            // Same key, different operation: a ticket-create token replayed as a message token.
            // The payloads are not even comparable, so this is a client bug rather than a
            // replay, and answering it from the stored row would return the wrong kind of result.
            log.warn("Idempotency key reused across operations: expected={} got={} actorKey={}",
                    key.getOperation(), operation, actorKey);
            throw new ConflictingReuseException(
                    "This idempotency key was already used for a different operation.");
        }

        if (!key.getRequestHash().equals(requestHash)) {
            // The dangerous case: same key, different content. Returning the stored result would
            // answer a question the client did not ask.
            log.warn("Idempotency key reused with a different payload: operation={} actorKey={}",
                    operation, actorKey);
            throw new ConflictingReuseException(
                    "This idempotency key was already used for a different request.");
        }

        if (!key.isCompleted()) {
            // In flight right now, or crashed too recently for the stale sweep. Neither is a
            // result, and pretending otherwise would hand back an acknowledgement for work that
            // may not exist.
            throw new ConflictingReuseException(
                    "A request with this idempotency key is still in progress. Retry shortly.");
        }

        return Outcome.replay(priorFetcher.fromPrior(
                new PriorResult(key.getResourceId(), key.getResourceText())));
    }

    /** Marks a claim as having produced a result, so a later retry can replay it. */
    private void complete(UUID claimId, UUID resourceId, String resourceText) {
        repository.complete(claimId, resourceId, resourceText, LocalDateTime.now());
    }

    /**
     * Retention sweep. Removes finished keys past their window and claims left behind by
     * requests that died, so the ledger cannot grow without bound.
     */
    @Transactional
    public int purgeExpired() {
        LocalDateTime now = LocalDateTime.now();
        int removed = repository.deleteExpiredCompleted(now);
        removed += repository.deleteExpiredUncompleted(now.minusDays(RETENTION_DAYS));
        if (removed > 0) {
            log.info("Purged {} expired communication idempotency keys", removed);
        }
        return removed;
    }

    // ── helpers ────────────────────────────────────────────────────────────────────────────

    /** Actor scope for an authenticated caller. */
    public static String actor(UUID userId) {
        return userId == null ? "anonymous" : userId.toString();
    }

    /** Actor scope for an anonymous caller: the strongest identity available without one. */
    public static String actorForAddress(String clientAddress) {
        return "ip:" + (clientAddress == null || clientAddress.isBlank() ? "unknown" : clientAddress);
    }

    /**
     * Hashes a payload for conflict detection.
     *
     * <p>Canonicalised by joining with a separator that cannot appear in the parts, so
     * ("ab","c") and ("a","bc") do not hash alike and produce a false conflict.</p>
     */
    public static String hashOf(String... parts) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String part : parts) {
                digest.update((part == null ? "" : part).getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0x1F); // unit separator
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            // SHA-256 is mandated by the platform; absence is not a runtime condition.
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    /**
     * Validates a client-supplied key.
     *
     * <p>Bounded and charset-checked before it reaches a column or a log line, so a malformed
     * header cannot become an injection vector or an unbounded row.</p>
     */
    public static Optional<String> normaliseKey(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String trimmed = raw.trim();
        if (trimmed.length() > 100 || !trimmed.matches("[A-Za-z0-9._:-]{8,100}")) {
            throw new ConflictingReuseException(
                    "Idempotency-Key must be 8-100 characters of letters, digits, '.', '_', ':' or '-'.");
        }
        return Optional.of(trimmed);
    }
}
