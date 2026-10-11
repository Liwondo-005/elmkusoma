package tz.elmkusoma.liveclass.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.course.domain.LiveClass;
import tz.elmkusoma.liveclass.domain.MediaAsset;
import tz.elmkusoma.liveclass.repository.MediaAssetRepository;
import tz.elmkusoma.teacher.domain.Teacher;
import tz.elmkusoma.teacher.repository.TeacherRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Publishes live-class recordings into the EXISTING media/video library
 * ({@code media_assets}) so a finished recording becomes a discoverable, institution-scoped
 * asset instead of only a {@code live_classes.recording_url} string.
 *
 * <p>This deliberately reuses the existing library: same table, same tenancy, same status and
 * visibility vocabulary, same lookups. No second media store, no duplicate storage path.
 *
 * <p>Idempotent: the asset is keyed on {@code (sourceType=LIVE_CLASS, sourceId=liveClassId)},
 * so repeated completion webhooks or an end-time finalize converge on one row.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LiveRecordingMediaPublisher {

    /** Source type linking a media asset back to the live class that produced it. */
    public static final String SOURCE_LIVE_CLASS = "LIVE_CLASS";
    public static final String MEDIA_TYPE_RECORDING = "RECORDING";
    public static final String STATUS_READY = "READY";
    public static final String STATUS_FAILED = "FAILED";

    private final MediaAssetRepository mediaAssetRepository;
    private final TeacherRepository teacherRepository;

    /**
     * Creates (or refreshes) the library asset for a completed recording.
     *
     * @param liveClass      the session the recording belongs to
     * @param recordingUrl   resolved storage URL of the finished recording
     * @param durationSeconds duration reported by LiveKit egress, when known
     * @return the persisted asset, or empty when there is nothing to publish
     */
    /**
     * Publishes a completed recording, tolerating a concurrent publisher.
     *
     * <p>V150 makes (source_type = LIVE_CLASS, source_id) unique in the database, so a genuine
     * race - a manual End racing the expiry sweep, or the same webhook retried onto a second
     * application instance - can no longer leave two assets for one recording. When that race is
     * lost, the unique violation means a sibling transaction already published the row, so the
     * existing asset is re-read and returned instead of surfacing an error to the caller.
     *
     * <p>The catch has to sit outside the transactional method: the losing transaction is already
     * marked rollback-only by the constraint violation, so it cannot be reused.
     */
    public Optional<MediaAsset> publishRecordingSafely(LiveClass liveClass, String recordingUrl, Long durationSeconds) {
        try {
            return publishRecording(liveClass, recordingUrl, durationSeconds);
        } catch (DataIntegrityViolationException e) {
            log.warn("Concurrent recording publish for class {} resolved to the existing asset", liveClass.getId());
            return findForLiveClass(liveClass == null ? null : liveClass.getId());
        }
    }

    /** Failure counterpart of {@link #publishRecordingSafely}. */
    public Optional<MediaAsset> markRecordingFailedSafely(LiveClass liveClass, String reason) {
        try {
            return markRecordingFailed(liveClass, reason);
        } catch (DataIntegrityViolationException e) {
            log.warn("Concurrent failure marking for class {} resolved to the existing asset", liveClass.getId());
            return findForLiveClass(liveClass == null ? null : liveClass.getId());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<MediaAsset> publishRecording(LiveClass liveClass, String recordingUrl, Long durationSeconds) {
        if (liveClass == null || liveClass.getId() == null || recordingUrl == null || recordingUrl.isBlank()) {
            return Optional.empty();
        }
        // The transient egress:<id> marker is LiveKit bookkeeping, not a playable URL, and must
        // never reach a learner-facing library.
        if (!recordingUrl.startsWith("http")) {
            log.warn("Refusing to publish non-http recording location for class {}: {}", liveClass.getId(), recordingUrl);
            return Optional.empty();
        }
        MediaAsset asset = findOrCreate(liveClass);
        asset.setFileUrl(recordingUrl);
        asset.setDurationSeconds(durationSeconds);
        asset.setStatus(STATUS_READY);
        asset.setVisibility("INSTITUTION");
        if (asset.getTitle() == null || asset.getTitle().isBlank()) {
            asset.setTitle(liveClass.getTitle());
        }
        if (asset.getDescription() == null || asset.getDescription().isBlank()) {
            asset.setDescription("Recording of the live class \"" + liveClass.getTitle() + "\"");
        }
        asset.setSubjectId(liveClass.getSubjectId());
        asset.setClassGroupId(liveClass.getClassGroupId());
        asset.setTeacherId(resolveTeacherUserId(liveClass));
        MediaAsset saved = mediaAssetRepository.save(asset);
        log.info("Live class recording published to media library: classId={}, assetId={}",
                liveClass.getId(), saved.getId());
        return Optional.of(saved);
    }

    /**
     * Records a failed recording as a real, queryable FAILED asset.
     * <p>
     * Previously a failed recording was log-only and left {@code recording_url} holding the
     * {@code egress:<id>} marker, which the download endpoint reports as "still processing"
     * forever. The marker is cleared here so the failure is visible instead of pending.
     *
     * @return the persisted FAILED asset
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<MediaAsset> markRecordingFailed(LiveClass liveClass, String reason) {
        if (liveClass == null || liveClass.getId() == null) {
            return Optional.empty();
        }
        MediaAsset asset = findOrCreate(liveClass);
        asset.setStatus(STATUS_FAILED);
        asset.setVisibility("INSTITUTION");
        if (asset.getTitle() == null || asset.getTitle().isBlank()) {
            asset.setTitle(liveClass.getTitle());
        }
        asset.setDescription("Recording failed for the live class \"" + liveClass.getTitle() + "\""
                + (reason == null || reason.isBlank() ? "" : " (" + reason + ")"));
        asset.setSubjectId(liveClass.getSubjectId());
        asset.setClassGroupId(liveClass.getClassGroupId());
        asset.setTeacherId(resolveTeacherUserId(liveClass));
        MediaAsset saved = mediaAssetRepository.save(asset);
        log.warn("Live class recording marked FAILED in media library: classId={}, assetId={}, reason={}",
                liveClass.getId(), saved.getId(), reason);
        return Optional.of(saved);
    }

    /** The library asset for this live class's recording, if one already exists. */
    @Transactional(readOnly = true)
    public Optional<MediaAsset> findForLiveClass(UUID liveClassId) {
        if (liveClassId == null) {
            return Optional.empty();
        }
        return mediaAssetRepository.findFirstBySourceTypeAndSourceIdAndIsDeletedFalse(
                SOURCE_LIVE_CLASS, liveClassId);
    }

    private MediaAsset findOrCreate(LiveClass liveClass) {
        Optional<MediaAsset> existing = mediaAssetRepository
                .findFirstBySourceTypeAndSourceIdAndIsDeletedFalse(SOURCE_LIVE_CLASS, liveClass.getId());
        if (existing.isPresent()) {
            return existing.get();
        }
        MediaAsset asset = MediaAsset.builder()
                .title(liveClass.getTitle())
                .mediaType(MEDIA_TYPE_RECORDING)
                .sourceType(SOURCE_LIVE_CLASS)
                .sourceId(liveClass.getId())
                .status(STATUS_READY)
                .visibility("INSTITUTION")
                .build();
        // BaseEntity is not part of the Lombok builder, so tenancy is set explicitly.
        asset.setInstitutionId(liveClass.getInstitutionId());
        return asset;
    }

    /**
     * LiveClass.teacherId is a {@code teachers.id}, but {@code media_assets.teacher_id} holds a
     * {@code users.id} (MediaLibraryController writes the authenticated user id), so the teacher
     * must be resolved rather than copied - otherwise the asset is filed under a foreign id.
     */
    private UUID resolveTeacherUserId(LiveClass liveClass) {
        if (liveClass.getTeacherId() == null) {
            return null;
        }
        return teacherRepository.findById(liveClass.getTeacherId())
                .map(Teacher::getUserId)
                .orElse(null);
    }
}