package tz.elmkusoma.liveclass.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import tz.elmkusoma.course.domain.LiveClass;
import tz.elmkusoma.course.repository.LiveClassRepository;
import tz.elmkusoma.event.repository.ReplayRepository;
import tz.elmkusoma.liveclass.domain.MediaAsset;
import tz.elmkusoma.liveclass.repository.MediaAssetRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Publication race between two paths that genuinely run concurrently: session completion and
 * the {@code egress_ended} webhook. They overlap on essentially every recording, because the
 * upload finishes around the moment the class ends.
 *
 * <p>Deliberately <em>not</em> {@code @Transactional}: a shared test transaction would give
 * every thread the same snapshot and hide the race entirely. Each thread runs its own
 * transaction here, which is what production does, and the assertion is the one that matters
 * to learners - exactly one Replay and one Media Asset.</p>
 *
 * <p>Both callers take the live class row lock first, so the "does it already exist?" check is
 * only ever made while another caller cannot be doing the same thing. The V150 unique indexes
 * are the guarantee if a future call path forgets that.</p>
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LiveRecordingPublisherConcurrencyTest {

    private static final String SRC = "LIVE_CLASS";
    private static final String LOCATION = "https://s3.us-east-005.backblazeb2.com/elmkusoma-live-recordings/recordings/race.mp4";
    private static final int CALLERS = 6;

    @Autowired private LiveRecordingPublisher publisher;
    @Autowired private LiveClassRepository liveClassRepository;
    @Autowired private ReplayRepository replayRepository;
    @Autowired private MediaAssetRepository mediaAssetRepository;
    @Autowired private JdbcTemplate jdbc;

    private final List<UUID> created = new ArrayList<>();

    @BeforeAll
    void createIdempotencyIndexes() {
        // Flyway is off for tests, so apply the V150 constraint directly. Not inside a
        // transaction: nothing here rolls back, and the index must outlive a single test.
        jdbc.execute("CREATE UNIQUE INDEX IF NOT EXISTS uq_replays_live_session "
                + "ON replays (live_session_id) WHERE live_session_id IS NOT NULL AND is_deleted = false");
        jdbc.execute("CREATE UNIQUE INDEX IF NOT EXISTS uq_media_assets_source "
                + "ON media_assets (source_type, source_id) "
                + "WHERE source_type IS NOT NULL AND source_id IS NOT NULL AND is_deleted = false");
    }

    @BeforeEach
    void reset() {
        cleanup();
    }

    @AfterEach
    void tearDown() {
        cleanup();
    }

    private void cleanup() {
        for (UUID id : created) {
            replayRepository.findByLiveSessionIdAndIsDeletedFalse(id).forEach(replayRepository::delete);
            mediaAssetRepository.findBySourceTypeAndSourceIdAndIsDeletedFalse(SRC, id).forEach(mediaAssetRepository::delete);
            liveClassRepository.deleteById(id);
        }
        created.clear();
    }

    private UUID newLiveClass() {
        LiveClass lc = LiveClass.builder()
                .title("Publication race test")
                .teacherId(UUID.randomUUID())
                .scheduledAt(LocalDateTime.now().minusHours(1))
                // @SuperBuilder ignores the field's `= 60` initializer (no @Builder.Default),
                // so it must be passed explicitly or the NOT NULL column gets null.
                .durationMinutes(60)
                .status("ENDED")
                .build();
        lc.setRecordingUrl("egress:EG_race");
        UUID id = liveClassRepository.save(lc).getId();
        created.add(id);
        return id;
    }

    @Test
    void concurrentCallersProduceExactlyOneReplayAndOneAsset() throws Exception {
        UUID classId = newLiveClass();

        ExecutorService pool = Executors.newFixedThreadPool(CALLERS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> outcomes = new ArrayList<>();

        try {
            for (int i = 0; i < CALLERS; i++) {
                outcomes.add(pool.submit(() -> {
                    start.await();
                    return publisher.publishResolved(classId, LOCATION);
                }));
            }
            start.countDown();

            int published = 0;
            for (Future<Boolean> outcome : outcomes) {
                if (outcome.get(60, TimeUnit.SECONDS)) {
                    published++;
                }
            }

            assertTrue(published >= 1, "at least one caller must report success");
            assertEquals(1, replayRepository.findByLiveSessionIdAndIsDeletedFalse(classId).size(),
                    "concurrent publication must yield exactly one Replay");
            assertEquals(1, mediaAssetRepository.findBySourceTypeAndSourceIdAndIsDeletedFalse(SRC, classId).size(),
                    "concurrent publication must yield exactly one Media Asset");
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void concurrentReprocessingOverAnAlreadyResolvedUrlIsAlsoIdempotent() throws Exception {
        UUID classId = newLiveClass();
        // Webhook already resolved it; now session completion and a webhook redelivery race.
        publisher.publishResolved(classId, LOCATION);

        ExecutorService pool = Executors.newFixedThreadPool(CALLERS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> outcomes = new ArrayList<>();

        try {
            for (int i = 0; i < CALLERS; i++) {
                final boolean viaSessionCompletion = i % 2 == 0;
                outcomes.add(pool.submit(() -> {
                    start.await();
                    return viaSessionCompletion
                            ? publisher.resolveAndPublish(classId)
                            : publisher.publishResolved(classId, LOCATION);
                }));
            }
            start.countDown();
            for (Future<Boolean> outcome : outcomes) {
                outcome.get(60, TimeUnit.SECONDS);
            }

            assertEquals(1, replayRepository.findByLiveSessionIdAndIsDeletedFalse(classId).size(),
                    "mixed session-end/webhook concurrency must not duplicate the Replay");
            assertEquals(1, mediaAssetRepository.findBySourceTypeAndSourceIdAndIsDeletedFalse(SRC, classId).size(),
                    "mixed session-end/webhook concurrency must not duplicate the Media Asset");
        } finally {
            pool.shutdownNow();
        }
    }
}
