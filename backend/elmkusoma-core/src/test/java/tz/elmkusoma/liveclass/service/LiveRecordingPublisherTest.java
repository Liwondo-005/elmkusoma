package tz.elmkusoma.liveclass.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.course.domain.LiveClass;
import tz.elmkusoma.course.repository.LiveClassRepository;
import tz.elmkusoma.event.domain.Replay;
import tz.elmkusoma.event.repository.ReplayRepository;
import tz.elmkusoma.liveclass.domain.MediaAsset;
import tz.elmkusoma.liveclass.repository.MediaAssetRepository;
import tz.elmkusoma.testutil.TestDataSeeder;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Live class recording publication: the ordering, duplication and failure cases that decide
 * whether a finished recording is filed exactly once or not at all.
 *
 * <p>Two independent paths publish a recording. Session completion (manual end or the expiry
 * sweep) stops the egress and polls for a URL for a bounded window; the LiveKit
 * {@code egress_ended} webhook carries the same location whenever LiveKit finishes. With
 * object storage the upload takes far longer than that polling window, so on any real
 * recording the class is <em>already ended</em> when the webhook lands - which the old
 * handler did not handle: it wrote {@code recording_url} and stopped, leaving no Replay and
 * no MediaAsset. The same handler now runs the identical publication path, which is what
 * makes every ordering converge.</p>
 *
 * <p>Tests are transactional and roll back, so each starts from a clean live class. Flyway is
 * disabled for tests ({@code application-test.properties}), so the V150 unique indexes are
 * applied by hand in {@code @BeforeEach} - the constraint being asserted must be the real
 * one.</p>
 */
@SpringBootTest
@Import(TestDataSeeder.class)
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class LiveRecordingPublisherTest {

    private static final String SRC = "LIVE_CLASS";
    private static final String LOCATION = "https://s3.us-east-005.backblazeb2.com/elmkusoma-live-recordings/recordings/cls-1.mp4";

    @Autowired private LiveRecordingPublisher publisher;
    @Autowired private LiveClassRepository liveClassRepository;
    @Autowired private ReplayRepository replayRepository;
    @Autowired private MediaAssetRepository mediaAssetRepository;
    @Autowired private JdbcTemplate jdbc;

    private UUID classId;

    @BeforeEach
    void setUp() {
        jdbc.execute("CREATE UNIQUE INDEX IF NOT EXISTS uq_replays_live_session "
                + "ON replays (live_session_id) WHERE live_session_id IS NOT NULL AND is_deleted = false");
        jdbc.execute("CREATE UNIQUE INDEX IF NOT EXISTS uq_media_assets_source "
                + "ON media_assets (source_type, source_id) "
                + "WHERE source_type IS NOT NULL AND source_id IS NOT NULL AND is_deleted = false");

        classId = newLiveClass("https://s3.us-east-005.backblazeb2.com/elmkusoma-live-recordings/recordings/pending.mp4");
    }

    /** A class that has already run, with an egress marker still on the recording URL. */
    private UUID newLiveClass(String recordingUrl) {
        LiveClass lc = LiveClass.builder()
                .title("Recording publication test")
                // live_classes.teacher_id is a real FK to teachers (LiveClass maps a @ManyToOne
                // Teacher), so the seeded teacher must be used rather than a random UUID.
                .teacherId(TestDataSeeder.TEACHER_ID)
                .scheduledAt(LocalDateTime.of(2026, 1, 12, 9, 0))
                // @SuperBuilder ignores the field's `= 60` initializer (no @Builder.Default),
                // so it must be passed explicitly or the NOT NULL column gets null.
                .durationMinutes(60)
                .status("ENDED")
                .build();
        lc.setRecordingUrl(recordingUrl);
        return liveClassRepository.save(lc).getId();
    }

    private int replayCount() {
        return replayRepository.findByLiveSessionIdAndIsDeletedFalse(classId).size();
    }

    private int assetCount() {
        return mediaAssetRepository.findBySourceTypeAndSourceIdAndIsDeletedFalse(SRC, classId).size();
    }

    private String storedLocation() {
        return liveClassRepository.findById(classId).orElseThrow().getRecordingUrl();
    }

    // ---- the late webhook: the failure this fix exists for ------------------------

    @Test
    void webhookArrivingAfterSessionEndStillFilesTheRecording() {
        // Session end already gave up: the class is ENDED, the marker never resolved, and
        // no Replay and no MediaAsset exist. This is the state the recording was left in.
        classId = newLiveClass("egress:EG_late");
        assertFalse(publisher.resolveAndPublish(classId), "egress is disabled in tests, so nothing resolves");
        assertEquals(0, replayCount(), "session end must not create a Replay it cannot link to");
        assertEquals(0, assetCount());

        // The late webhook carries the real location.
        assertTrue(publisher.publishResolved(classId, LOCATION), "the webhook must be able to finish publication");

        assertEquals(1, replayCount(), "a Replay must exist for a recording that is in storage");
        assertEquals(1, assetCount(), "a Media Asset must exist for a recording that is in storage");
        assertEquals(LOCATION, storedLocation(), "the resolved location must be persisted");
        assertEquals("AVAILABLE",
                replayRepository.findByLiveSessionIdAndIsDeletedFalse(classId).get(0).getStatus());
        assertEquals("READY",
                mediaAssetRepository.findBySourceTypeAndSourceIdAndIsDeletedFalse(SRC, classId).get(0).getStatus());
    }

    // ---- duplicate and repeated delivery ------------------------------------------

    @Test
    void repeatedWebhookDeliveryDoesNotDuplicateRecords() {
        assertTrue(publisher.publishResolved(classId, LOCATION));
        assertTrue(publisher.publishResolved(classId, LOCATION));
        assertTrue(publisher.publishResolved(classId, LOCATION));

        assertEquals(1, replayCount(), "a duplicate webhook must not create a second Replay");
        assertEquals(1, assetCount(), "a duplicate webhook must not create a second Media Asset");
    }

    // ---- both orderings -----------------------------------------------------------

    @Test
    void sessionCompletionAfterTheWebhookDoesNotDuplicateRecords() {
        // Webhook first: the URL is real before the class ends.
        assertTrue(publisher.publishResolved(classId, LOCATION));

        // Session completion then runs its normal path over an already-resolved URL.
        assertTrue(publisher.resolveAndPublish(classId));

        assertEquals(1, replayCount(), "webhook-then-session-end must not duplicate the Replay");
        assertEquals(1, assetCount(), "webhook-then-session-end must not duplicate the Media Asset");
        assertEquals(LOCATION, storedLocation());
    }

    @Test
    void sessionCompletionBeforeTheWebhookIsStillFollowedByAWorkingWebhook() {
        // Session end resolves and publishes...
        assertTrue(publisher.resolveAndPublish(classId));
        assertEquals(1, replayCount());
        assertEquals(1, assetCount());

        // ...and the webhook re-delivers the same location.
        assertTrue(publisher.publishResolved(classId, LOCATION));
        assertEquals(1, replayCount(), "session-end-then-webhook must not duplicate the Replay");
        assertEquals(1, assetCount(), "session-end-then-webhook must not duplicate the Media Asset");
    }

    // ---- storage / configuration failures -----------------------------------------

    @Test
    void unresolvedEgressLeavesTheRecordingPendingSoTheWebhookCanStillFinishIt() {
        classId = newLiveClass("egress:EG_pending");

        assertFalse(publisher.resolveAndPublish(classId), "storage is not configured in tests");
        assertEquals(0, replayCount());
        assertEquals(0, assetCount());
        assertEquals("egress:EG_pending", storedLocation(),
                "the marker must survive so the webhook still knows what to file");

        // ...and then the webhook arrives.
        assertTrue(publisher.publishResolved(classId, LOCATION));
        assertEquals(1, replayCount());
        assertEquals(1, assetCount());
    }

    @Test
    void aClassWithNoRecordingAtAllIsNotGivenOne() {
        UUID untouched = newLiveClass(null);

        assertFalse(publisher.resolveAndPublish(untouched));
        assertFalse(publisher.publishResolved(untouched, LOCATION),
                "publication must not be reachable for a class that never recorded");
        assertEquals(0, replayRepository.findByLiveSessionIdAndIsDeletedFalse(untouched).size());
        assertEquals(0, mediaAssetRepository.findBySourceTypeAndSourceIdAndIsDeletedFalse(SRC, untouched).size());
    }

    @Test
    void missingLocationCreatesNothingAndStoresNothing() {
        assertFalse(publisher.publishResolved(classId, null));
        assertFalse(publisher.publishResolved(classId, ""));
        assertFalse(publisher.publishResolved(classId, "   "));

        assertEquals(0, replayCount());
        assertEquals(0, assetCount());
        assertEquals("https://s3.us-east-005.backblazeb2.com/elmkusoma-live-recordings/recordings/pending.mp4",
                storedLocation(), "an unusable location must never overwrite a known one");
    }

    @Test
    void unusableLocationCreatesNothingAndInventsNoUrl() {
        // Not http, and tests configure no egress endpoint to rebuild against - so the only
        // correct outcome is "did not publish", with no fabricated URL written anywhere.
        assertFalse(publisher.publishResolved(classId, "not-a-location"));

        assertEquals(0, replayCount());
        assertEquals(0, assetCount());
        assertEquals("https://s3.us-east-005.backblazeb2.com/elmkusoma-live-recordings/recordings/pending.mp4",
                storedLocation(), "an unusable location must not be persisted");
    }

    @Test
    void unknownLiveClassIsRejectedRatherThanSilentlySucceeded() {
        assertFalse(publisher.resolveAndPublish(UUID.randomUUID()));
        assertFalse(publisher.publishResolved(UUID.randomUUID(), LOCATION));
    }

    // ---- reprocessing repairs rather than duplicates -------------------------------

    @Test
    void reprocessingRefreshesTheExistingAssetInsteadOfAddingAnother() {
        assertTrue(publisher.publishResolved(classId, LOCATION));
        MediaAsset first = mediaAssetRepository.findBySourceTypeAndSourceIdAndIsDeletedFalse(SRC, classId).get(0);

        String refreshed = "https://s3.us-east-005.backblazeb2.com/elmkusoma-live-recordings/recordings/cls-1-v2.mp4";
        assertTrue(publisher.publishResolved(classId, refreshed), "a repaired Replay counts as published");

        assertEquals(1, assetCount());
        MediaAsset second = mediaAssetRepository.findBySourceTypeAndSourceIdAndIsDeletedFalse(SRC, classId).get(0);
        assertEquals(first.getId(), second.getId(), "the existing asset must be reused, not recreated");
        assertEquals(refreshed, second.getFileUrl());
    }

    // ---- what actually gets written ------------------------------------------------

    @Test
    void publishedAssetAndReplayAreKeyedOnTheOwningClassAndInstitution() {
        UUID institutionId = UUID.randomUUID();
        UUID classGroupId = UUID.randomUUID();
        LiveClass live = liveClassRepository.findById(classId).orElseThrow();
        live.setInstitutionId(institutionId);
        live.setClassGroupId(classGroupId);
        liveClassRepository.save(live);

        assertTrue(publisher.publishResolved(classId, LOCATION));

        MediaAsset asset = mediaAssetRepository.findBySourceTypeAndSourceIdAndIsDeletedFalse(SRC, classId).get(0);
        assertEquals("RECORDING", asset.getMediaType());
        assertEquals(LOCATION, asset.getFileUrl());
        assertEquals(SRC, asset.getSourceType());
        assertEquals(classId, asset.getSourceId(), "must be keyed on the originating live class");
        assertEquals(classGroupId, asset.getClassGroupId(), "recording must target its class");
        assertEquals(institutionId, asset.getInstitutionId(), "ownership comes from the live class");
        assertEquals(live.getTitle(), asset.getTitle());

        Replay replay = replayRepository.findByLiveSessionIdAndIsDeletedFalse(classId).get(0);
        assertEquals(classId, replay.getLiveSessionId());
        assertEquals(LOCATION, replay.getRecordingUrl());
        assertEquals(institutionId, replay.getInstitutionId());
    }

    /** A non-http scheme is not a playable recording and must not be published as one. */
    @Test
    void nonHttpLocationSchemeIsRejected() {
        assertFalse(publisher.publishResolved(classId, "file:///tmp/rec.mp4"));

        assertEquals(0, replayCount());
        assertEquals(0, assetCount());
        assertEquals("https://s3.us-east-005.backblazeb2.com/elmkusoma-live-recordings/recordings/pending.mp4",
                storedLocation());
    }

    // ---- the database guarantee behind all of the above ----------------------------

    @Test
    void databaseRefusesASecondReplayForTheSameSession() {
        // Proves the V150 constraint bites even if a future call path bypasses the row lock
        // in LiveRecordingPublisher. Without it, two callers that check at the same moment
        // both insert.
        replayRepository.save(Replay.builder()
                .liveSessionId(classId)
                .title("first")
                .recordingUrl(LOCATION)
                .status("AVAILABLE")
                .viewCount(0)
                .lastPositionSeconds(0)
                .build());

        Replay duplicate = Replay.builder()
                .liveSessionId(classId)
                .title("second")
                .recordingUrl(LOCATION)
                .status("AVAILABLE")
                .viewCount(0)
                .lastPositionSeconds(0)
                .build();

        assertThrows(Exception.class, () -> replayRepository.saveAndFlush(duplicate),
                "replays(live_session_id) must be unique for a live, non-deleted replay");
    }

    @Test
    void databaseRefusesASecondAssetForTheSameSource() {
        MediaAsset first = MediaAsset.builder()
                .title("first")
                .mediaType("RECORDING")
                .fileUrl(LOCATION)
                .status("READY")
                .visibility("INSTITUTION")
                .sourceType(SRC)
                .sourceId(classId)
                .build();
        mediaAssetRepository.saveAndFlush(first);

        MediaAsset duplicate = MediaAsset.builder()
                .title("second")
                .mediaType("RECORDING")
                .fileUrl(LOCATION)
                .status("READY")
                .visibility("INSTITUTION")
                .sourceType(SRC)
                .sourceId(classId)
                .build();

        assertThrows(Exception.class, () -> mediaAssetRepository.saveAndFlush(duplicate),
                "media_assets(source_type, source_id) must be unique for a live, non-deleted asset");
    }
}
