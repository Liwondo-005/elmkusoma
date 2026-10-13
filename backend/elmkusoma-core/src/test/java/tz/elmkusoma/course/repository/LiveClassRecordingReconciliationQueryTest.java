package tz.elmkusoma.course.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.course.domain.LiveClass;
import tz.elmkusoma.testutil.TestDataSeeder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The reconciliation sweep must only ever look at classes that have actually ended.
 *
 * <p>{@code findWithUnresolvedRecordingMarker} feeds {@code reconcilePendingRecordings}, which calls
 * {@code finalizeRecordingAndCreateReplay} - and that issues {@code StopEgress} for any
 * {@code egress:<id>} marker it finds. The javadoc promised "ended classes" but the query filtered
 * nothing but the marker, so every class currently recording was a candidate: a background sweep
 * running every couple of minutes could silently cut short a live recording nobody asked to stop,
 * and publish the truncated file.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestDataSeeder.class)
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class LiveClassRecordingReconciliationQueryTest {

    @Autowired private LiveClassRepository liveClassRepository;

    private LiveClass classWith(String status, LocalDateTime endedAt, String recordingUrl) {
        return liveClassRepository.saveAndFlush(LiveClass.builder()
                .institutionId(TestDataSeeder.INSTITUTION_ID)
                .teacherId(TestDataSeeder.TEACHER_ID)
                .title("Reconcile " + UUID.randomUUID().toString().substring(0, 8))
                .scheduledAt(LocalDateTime.now().minusHours(2))
                .durationMinutes(60)
                .status(status)
                .endedAt(endedAt)
                .recordingUrl(recordingUrl)
                .isDeleted(false)
                .build());
    }

    private List<LiveClass> sweep() {
        return liveClassRepository.findWithUnresolvedRecordingMarker(PageRequest.of(0, 25));
    }

    private boolean contains(List<LiveClass> rows, LiveClass target) {
        return rows.stream().anyMatch(r -> r.getId().equals(target.getId()));
    }

    @Test
    void endedClassStillHoldingTheMarker_isReturned() {
        LiveClass ended = classWith("COMPLETED", LocalDateTime.now(), "egress:eg-ended");

        assertTrue(contains(sweep(), ended),
                "an ended class whose upload never resolved is exactly what the sweep is for");
    }

    /**
     * The defect this test exists for: a class that is still LIVE holds the same marker, and the
     * sweep used to hand it straight to a method that stops its egress.
     */
    @Test
    void liveClassStillRecording_isNeverReturned() {
        LiveClass live = classWith("IN_PROGRESS", null, "egress:eg-live");
        LiveClass starting = classWith("STARTING", null, "egress:eg-starting");

        List<LiveClass> rows = sweep();

        assertFalse(contains(rows, live),
                "the sweep must never stop a recording of a class that is still running");
        assertFalse(contains(rows, starting),
                "the sweep must never stop a recording of a class that has not started yet");
    }

    /** Sessions ended before ended_at existed are still recoverable through their status. */
    @Test
    void endedClassWithoutEndedAt_isStillRecoverableByStatus() {
        LiveClass legacy = classWith("ENDED", null, "egress:eg-legacy");

        assertTrue(contains(sweep(), legacy),
                "a session ended before ended_at was added must not be stranded forever");
    }

    @Test
    void endedClassWithoutAMarker_isNotReturned() {
        LiveClass published = classWith("COMPLETED", LocalDateTime.now(), "https://cdn.test/done.mp4");
        LiveClass neverRecorded = classWith("COMPLETED", LocalDateTime.now(), null);

        List<LiveClass> rows = sweep();

        assertFalse(contains(rows, published), "a class whose location is already resolved is done");
        assertFalse(contains(rows, neverRecorded), "a class with nothing to reconcile is noise");
    }

    /** A cancelled class has no recording to recover, marker or not. */
    @Test
    void cancelledClass_isNotReturned() {
        LiveClass cancelled = classWith("CANCELLED", null, "egress:eg-cancelled");

        assertFalse(contains(sweep(), cancelled),
                "a cancelled session has no recording the sweep should try to stop or publish");
    }
}
