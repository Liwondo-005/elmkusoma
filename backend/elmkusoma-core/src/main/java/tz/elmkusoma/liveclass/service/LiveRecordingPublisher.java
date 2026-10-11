package tz.elmkusoma.liveclass.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import tz.elmkusoma.audit.domain.AuditLog;
import tz.elmkusoma.audit.service.AuditService;
import tz.elmkusoma.course.domain.LiveClass;
import tz.elmkusoma.course.repository.LiveClassRepository;
import tz.elmkusoma.event.domain.Replay;
import tz.elmkusoma.event.repository.ReplayRepository;
import tz.elmkusoma.learner.service.NotificationService;
import tz.elmkusoma.liveclass.domain.MediaAsset;
import tz.elmkusoma.liveclass.repository.MediaAssetRepository;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.teacher.domain.Teacher;
import tz.elmkusoma.teacher.repository.TeacherRepository;

import java.util.Map;
import java.util.UUID;

/**
 * Publishes a finished live class recording to the learner-facing surfaces: the Replay row and
 * the Media Library asset.
 *
 * <p>This lives in its own bean rather than inside {@code LiveClassServiceImpl} because it has
 * two callers that must both be able to run it: session completion, and the LiveKit
 * {@code egress_ended} webhook. With object storage the upload usually finishes <em>after</em>
 * the class ends - far longer than the in-request polling window - so the webhook is the normal
 * completion path and session completion is the fallback, not the other way round.</p>
 *
 * <p><strong>Re-entrant and idempotent.</strong> Both callers may run, in either order, and
 * LiveKit may deliver the same webhook more than once. Every entry point converges on the same
 * serialised section (see {@link LiveClassRepository#findByIdForUpdate}), so repeated and
 * overlapping calls converge to exactly one Replay and one MediaAsset instead of duplicating
 * them.</p>
 *
 * <p><strong>Never fabricates state.</strong> A location that cannot be turned into a URL is
 * reported as a failure rather than stored, and a recording problem never propagates: ending a
 * class must not fail because its recording could not be filed.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LiveRecordingPublisher {

    /** Media Library values for a finalized live class recording. */
    private static final String MEDIA_TYPE_RECORDING = "RECORDING";
    private static final String MEDIA_SOURCE_TYPE_LIVE_CLASS = "LIVE_CLASS";

    /**
     * Placeholder stored on {@code live_classes.recording_url} while the egress is still
     * running; the real location replaces it once LiveKit reports a file result.
     */
    private static final String EGRESS_URL_PREFIX = "egress:";

    private final LiveClassRepository liveClassRepository;
    private final LiveKitService liveKitService;
    private final ReplayRepository replayRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final PlatformTransactionManager transactionManager;

    /**
     * Resolves a still-pending recording and publishes it.
     *
     * <p>Used by session completion. If the recording is still marked {@code egress:<id>} the
     * egress is stopped and its location polled for; if a webhook already resolved the URL,
     * that URL is published as-is. When neither yields a location the recording is left
     * pending and the webhook remains responsible for finishing it.</p>
     *
     * @return true when a Replay and Media Asset are in place afterwards
     */
    public boolean resolveAndPublish(UUID liveClassId) {
        if (liveClassId == null) {
            return false;
        }
        String marker = liveClassRepository.findById(liveClassId)
                .map(LiveClass::getRecordingUrl)
                .orElse(null);
        if (marker == null || marker.isBlank()) {
            return false;
        }
        if (!marker.startsWith(EGRESS_URL_PREFIX)) {
            // Already a real location (webhook beat session end, or a reprocessing pass).
            return publishResolved(liveClassId, marker);
        }

        String egressId = marker.substring(EGRESS_URL_PREFIX.length());
        String resolved = resolveFromEgress(liveClassId, egressId);
        if (resolved == null) {
            // Not an error: the upload is simply still running. The egress_ended webhook
            // calls publishResolved when the object is actually in storage.
            log.warn("Recording not yet resolvable for class {} (egress {}); deferring publication to the egress_ended webhook",
                    liveClassId, egressId);
            return false;
        }
        return publishResolved(liveClassId, resolved);
    }

    /**
     * Publishes a recording whose location LiveKit has already reported.
     *
     * <p>This is the {@code egress_ended} webhook path. It is safe to call any number of times:
     * an existing Media Asset is refreshed in place rather than duplicated, and an existing
     * Replay is left untouched.</p>
     *
     * @param location the raw {@code file_results[].location} value from LiveKit; it is
     *                 normalised through {@link LiveKitService#toPlayableRecordingUrl(String)}
     *                 rather than assumed to be a URL
     * @return true when a Replay and Media Asset are in place afterwards
     */
    public boolean publishResolved(UUID liveClassId, String location) {
        if (liveClassId == null) {
            return false;
        }
        String url = liveKitService.toPlayableRecordingUrl(location);
        if (url == null) {
            log.warn("Cannot publish recording for class {}: location could not be resolved to a URL", liveClassId);
            return false;
        }
        try {
            // PROPAGATION_REQUIRED on purpose: when called from session completion this joins
            // that transaction, so it must not suspend it. Suspending would mean this method
            // re-acquires the very live class row the caller already holds, and the two would
            // wait on each other.
            return Boolean.TRUE.equals(new TransactionTemplate(transactionManager)
                    .execute(status -> publishUnderLock(liveClassId, url)));
        } catch (Exception e) {
            log.warn("Recording publication failed for class {}: {}", liveClassId, e.getMessage());
            return false;
        }
    }

    /**
     * LiveKit calls only - this method does no database work of its own, so it adds no
     * locks or writes while polling.
     *
     * <p>It does run inside whatever transaction the caller has open, so session completion's
     * transaction stays open for the length of the poll window (bounded by
     * {@link LiveKitService#resolveRecordingUrl}). That is deliberate: suspending the
     * transaction here would make the publication below re-acquire the live class row the
     * caller already holds, and the two would wait on each other. The bounded poll is also
     * no longer load-bearing - a timeout simply leaves the recording pending for the
     * {@code egress_ended} webhook to finish.</p>
     */
    private String resolveFromEgress(UUID liveClassId, String egressId) {
        try {
            // Stopping is best-effort: it may already have stopped, and either way the
            // location is still readable.
            liveKitService.stopRecording(egressId);
            return liveKitService.resolveRecordingUrl(egressId);
        } catch (Exception e) {
            log.warn("Egress resolution threw for class {} (egress {}): {}", liveClassId, egressId, e.getMessage());
            return null;
        }
    }

    /**
     * The single serialised publication section. Runs while holding the live class row lock,
     * so concurrent callers queue here instead of both deciding the record is missing.
     */
    private boolean publishUnderLock(UUID liveClassId, String url) {
        LiveClass liveClass = liveClassRepository.findByIdForUpdate(liveClassId).orElse(null);
        if (liveClass == null) {
            log.warn("Cannot publish recording: live class {} no longer exists", liveClassId);
            return false;
        }
        if (liveClass.getIsDeleted()) {
            log.warn("Cannot publish recording: live class {} is deleted", liveClassId);
            return false;
        }
        // A class that never started a recording has nothing to publish. A stray or replayed
        // webhook must not conjure a Replay for it, because a Replay is learner-facing.
        if (liveClass.getRecordingUrl() == null || liveClass.getRecordingUrl().isBlank()) {
            log.warn("Ignoring recording publication for class {}: it has no recording in progress",
                    liveClassId);
            return false;
        }

        // Persist the resolved location first: it is what survives if publication is
        // interrupted, and the next call resumes from here.
        final LiveClass current;
        if (url.equals(liveClass.getRecordingUrl())) {
            current = liveClass;
        } else {
            liveClass.setRecordingUrl(url);
            current = liveClassRepository.save(liveClass);
        }

        // Media Library first, on purpose: reprocessing a session whose Replay already exists
        // must still be able to repair a missing or stale library entry. It is idempotent.
        boolean assetPublished = safe("media library", liveClassId, () -> publishToMediaLibrary(current, url));
        boolean replayPublished = safe("replay", liveClassId, () -> publishReplay(current, url));
        return assetPublished || replayPublished;
    }

    /**
     * Runs one publication step in isolation. A failure here must not prevent the other step
     * from running - a missing Replay with a present asset is recoverable by the next call,
     * whereas a rolled-back pair is simply lost.
     */
    private boolean safe(String step, UUID liveClassId, java.util.function.BooleanSupplier action) {
        try {
            return action.getAsBoolean();
        } catch (Exception e) {
            log.warn("Recording publication step '{}' failed for class {}: {}", step, liveClassId, e.getMessage());
            return false;
        }
    }

    private boolean publishToMediaLibrary(LiveClass liveClass, String url) {
        // A soft-deleted asset stays deleted: an explicit library deletion is not undone
        // by reprocessing. Consistent with how Replay treats deleted rows.
        var existing = mediaAssetRepository.findBySourceTypeAndSourceIdAndIsDeletedFalse(
                MEDIA_SOURCE_TYPE_LIVE_CLASS, liveClass.getId());

        String title = liveClass.getTitle() != null ? liveClass.getTitle() : liveClass.getId().toString();
        String description = "Live class recording - " + title;

        if (!existing.isEmpty()) {
            MediaAsset asset = existing.get(0);
            asset.setFileUrl(url);
            asset.setTitle(title);
            asset.setDescription(description);
            if (liveClass.getClassGroupId() != null) {
                asset.setClassGroupId(liveClass.getClassGroupId());
            }
            mediaAssetRepository.save(asset);
            return true; // already present, refreshed in place
        }

        MediaAsset asset = MediaAsset.builder()
                .title(title)
                .description(description)
                .mediaType(MEDIA_TYPE_RECORDING)
                .fileUrl(url)
                // durationSeconds is deliberately left null: LiveKit's resolved URL carries no
                // recording length, and live_classes.recording_duration_seconds is never written
                // by any code, so any value here would be fabricated. Same for size and mime.
                .status("READY")
                .visibility("INSTITUTION")
                .sourceType(MEDIA_SOURCE_TYPE_LIVE_CLASS)
                .sourceId(liveClass.getId())
                .subjectId(liveClass.getSubjectId())
                .classGroupId(liveClass.getClassGroupId())
                // media_assets.teacher_id holds a users.id (MediaLibraryController writes the
                // authenticated user id), so the teacher must be resolved, not copied.
                .teacherId(resolveTeacherUserId(liveClass))
                .build();
        asset.setInstitutionId(liveClass.getInstitutionId());
        mediaAssetRepository.save(asset);
        log.info("Media Library recording published for live class {}: classGroupId={}, institutionId={}",
                liveClass.getId(), liveClass.getClassGroupId(), liveClass.getInstitutionId());
        return true;
    }

    /**
     * Returns true when a Replay exists for this session afterwards - whether this call
     * created it or an earlier one did. The contract of {@code publishResolved} is the end
     * state, so an idempotent re-run reports success exactly like a first run.
     */
    private boolean publishReplay(LiveClass liveClass, String url) {
        if (!replayRepository.findByLiveSessionIdAndIsDeletedFalse(liveClass.getId()).isEmpty()) {
            return true; // already published by an earlier call
        }

        Replay replay = Replay.builder()
                .liveSessionId(liveClass.getId())
                .eventId(null)
                .title(liveClass.getTitle())
                .description("Live class recording - " + (liveClass.getTitle() != null ? liveClass.getTitle() : liveClass.getId()))
                .recordingUrl(url)
                .status("AVAILABLE")
                .viewCount(0)
                .lastPositionSeconds(0)
                .institutionId(liveClass.getInstitutionId())
                .build();
        replayRepository.save(replay);
        log.info("Replay created for live class {}", liveClass.getId());
        writeSideEffects(liveClass, replay);
        return true;
    }

    /**
     * Fires the notification and audit write, each in its own transaction.
     *
     * <p>They are collateral, not part of the recording's correctness, but they write to
     * tables with their own NOT NULL constraints. Left in the caller's transaction a single
     * rejected row poisons it: PostgreSQL marks the whole transaction aborted, so every later
     * statement fails and the Replay that had already been inserted is rolled back with it.
     * The {@code try/catch} that used to guard these writes could not prevent that - catching
     * the exception does not un-abort a transaction.</p>
     *
     * <p>Separate transactions mean a failed audit loses one audit row and nothing else. The
     * two are also kept apart from each other, so one failing does not suppress the other.</p>
     */
    private void writeSideEffects(LiveClass liveClass, Replay replay) {
        TransactionTemplate sideEffect = new TransactionTemplate(transactionManager);
        sideEffect.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        try {
            sideEffect.executeWithoutResult(status -> notifyReplayAvailable(liveClass));
        } catch (Exception ex) {
            log.warn("Replay available notification failed for live class {}: {}", liveClass.getId(), ex.getMessage());
        }
        try {
            sideEffect.executeWithoutResult(status -> auditReplayGenerated(liveClass, replay));
        } catch (Exception ex) {
            log.warn("Audit write failed for replay {}: {}", replay.getId(), ex.getMessage());
        }
    }

    /** LiveClass.teacherId is a teachers.id; media_assets.teacher_id stores a users.id. */
    private UUID resolveTeacherUserId(LiveClass liveClass) {
        if (liveClass.getTeacherId() == null) {
            return null;
        }
        return teacherRepository.findById(liveClass.getTeacherId())
                .map(Teacher::getUserId)
                .orElse(null);
    }

    /** Existing notification channel - fired only when a Replay is actually created. */
    private void notifyReplayAvailable(LiveClass liveClass) {
        if (notificationService == null || liveClass.getInstitutionId() == null
                || liveClass.getTeacherId() == null) {
            return;
        }
        UUID teacherUserId = teacherRepository.findById(liveClass.getTeacherId())
                .map(Teacher::getUserId).orElse(null);
        notificationService.notifyInstitutionStudentsExcluding(
                liveClass.getInstitutionId(), teacherUserId,
                "Recording Available",
                "The recording for \"" + liveClass.getTitle() + "\" is now available to watch.",
                "REPLAY_AVAILABLE", "live_class", liveClass.getId());
    }

    /**
     * Audit trail for recording/replay generation, via the existing audit service.
     * Throwing is deliberate: the failure must roll back only this transaction, which
     * {@link #writeSideEffects} has already isolated from the recording itself.
     */
    private void auditReplayGenerated(LiveClass liveClass, Replay replay) {
        if (auditService == null || liveClass.getInstitutionId() == null) {
            return;
        }
        UUID actorUserId = liveClass.getTeacherId() != null
                ? teacherRepository.findById(liveClass.getTeacherId()).map(Teacher::getUserId).orElse(null)
                : null;
        String actorEmail = actorUserId != null
                ? userRepository.findById(actorUserId).map(User::getEmail).orElse(null)
                : null;
        auditService.recordAuditLog(liveClass.getInstitutionId(), actorUserId, actorEmail, "TEACHER",
                "replay", replay.getId(), replay.getTitle(), AuditLog.AuditAction.CREATE,
                null, Map.of(
                        "liveClassId", String.valueOf(liveClass.getId()),
                        "recordingUrl", String.valueOf(replay.getRecordingUrl())));
    }
}
