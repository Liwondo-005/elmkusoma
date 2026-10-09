package tz.elmkusoma.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tz.elmkusoma.course.domain.LiveClass;
import tz.elmkusoma.course.repository.LiveClassRepository;
import tz.elmkusoma.liveclass.domain.LiveClassAttendanceDetail;
import tz.elmkusoma.liveclass.domain.LiveClassParticipant;
import tz.elmkusoma.liveclass.repository.LiveClassAttendanceDetailRepository;
import tz.elmkusoma.liveclass.repository.LiveClassParticipantRepository;
import tz.elmkusoma.testutil.TestDataSeeder;
import tz.elmkusoma.testutil.TestTokens;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Live analytics integrity:
 * <ul>
 *   <li>peakParticipants must be real concurrency, not the current online count;</li>
 *   <li>attendance watch time is derived server-side, so a client cannot claim an
 *       arbitrary {@code totalSeconds} / {@code percentage};</li>
 *   <li>attendance is written once per participant (no duplicate rows).</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class LiveAttendanceIntegrityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LiveClassRepository liveClassRepository;

    @Autowired
    private LiveClassParticipantRepository participantRepository;

    @Autowired
    private LiveClassAttendanceDetailRepository attendanceDetailRepository;

    @Test
    void peakConcurrency_isMaxOverlap_notCurrentOnline() {
        LocalDateTime t0 = LocalDateTime.now().minusHours(3);

        // A: 10:00-10:30, B: 10:15-10:45 (overlap 2), C: 10:50-11:00 (alone),
        // D: never left (still present at "now").
        LiveClassParticipant a = participant(t0, t0.plusMinutes(30));
        LiveClassParticipant b = participant(t0.plusMinutes(15), t0.plusMinutes(45));
        LiveClassParticipant c = participant(t0.plusMinutes(50), t0.plusMinutes(60));
        LiveClassParticipant d = participant(t0.plusMinutes(20), null);

        int peak = tz.elmkusoma.liveclass.controller.LiveSessionController
                .computePeakConcurrency(java.util.List.of(a, b, c, d));

        // At 10:20: A + B + D are all present -> 3.
        assertEquals(3, peak, "peak must reflect the maximum simultaneous presence");
        assertTrue(peak > 1, "a single online participant must not be reported as the peak when others overlapped");
    }

    @Test
    void peakConcurrency_isZeroWithoutParticipants() {
        assertEquals(0, tz.elmkusoma.liveclass.controller.LiveSessionController
                .computePeakConcurrency(java.util.List.of()));
    }

    @Test
    void attendanceDetail_ignoresClientSuppliedTotals() throws Exception {
        LiveClass lc = liveClassInProgress(60);
        // Participant joined 10 minutes ago.
        participantRepository.save(LiveClassParticipant.builder()
                .liveClassId(lc.getId())
                .userId(TestDataSeeder.STUDENT_USER_ID)
                .role("LEARNER")
                .joinedAt(LocalDateTime.now().minusMinutes(10))
                .isDeleted(false)
                .build());

        // Client claims an absurd watch time (10 hours) and 100% attendance.
        String body = "{\"totalSeconds\":36000,\"percentage\":100,\"joinedAt\":\"2000-01-01T00:00:00\","
                + "\"leftAt\":\"2099-01-01T00:00:00\"}";
        mockMvc.perform(post("/v1/live-session/classes/" + lc.getId() + "/attendance-detail")
                        .header("X-Institution-Id", TestDataSeeder.INSTITUTION_ID.toString())
                        .header("Authorization", "Bearer " + TestTokens.studentToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        LiveClassAttendanceDetail detail = attendanceDetailRepository
                .findByLiveClassIdAndUserIdAndIsDeletedFalse(lc.getId(), TestDataSeeder.STUDENT_USER_ID)
                .orElseThrow();

        assertNotNull(detail.getJoinedAt());
        // Client's 2000-01-01 join must be ignored: real join is ~10 minutes ago.
        assertTrue(detail.getJoinedAt().isAfter(LocalDateTime.now().minusMinutes(20)),
                "joinedAt must come from the recorded participant row, not the request body");
        // 10 real minutes of a 60-minute class is ~17%, never 100%.
        assertTrue(detail.getTotalSeconds() < 1200,
                "totalSeconds must be derived from real presence, was " + detail.getTotalSeconds());
        assertTrue(detail.getPercentage().doubleValue() < 50.0,
                "percentage must be derived from real presence, was " + detail.getPercentage());
    }

    @Test
    void attendanceDetail_isUpserted_notDuplicated() throws Exception {
        LiveClass lc = liveClassInProgress(60);
        participantRepository.save(LiveClassParticipant.builder()
                .liveClassId(lc.getId())
                .userId(TestDataSeeder.STUDENT_USER_ID)
                .role("LEARNER")
                .joinedAt(LocalDateTime.now().minusMinutes(5))
                .isDeleted(false)
                .build());

        String body = "{\"totalSeconds\":1}";
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/v1/live-session/classes/" + lc.getId() + "/attendance-detail")
                            .header("X-Institution-Id", TestDataSeeder.INSTITUTION_ID.toString())
                            .header("Authorization", "Bearer " + TestTokens.studentToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated());
        }

        long rows = attendanceDetailRepository.findByLiveClassIdAndIsDeletedFalse(lc.getId()).stream()
                .filter(d -> TestDataSeeder.STUDENT_USER_ID.equals(d.getUserId()))
                .count();
        assertEquals(1, rows, "repeated attendance posts must update one row, not create duplicates");
    }

    @Test
    void attendanceDetail_nonParticipant_isRejected() throws Exception {
        LiveClass lc = liveClassInProgress(60);
        mockMvc.perform(post("/v1/live-session/classes/" + lc.getId() + "/attendance-detail")
                        .header("X-Institution-Id", TestDataSeeder.INSTITUTION_ID.toString())
                        .header("Authorization", "Bearer " + TestTokens.otherStudentToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"totalSeconds\":9999}"))
                .andExpect(status().isForbidden());
    }

    private LiveClass liveClassInProgress(int minutes) {
        return liveClassRepository.save(LiveClass.builder()
                .institutionId(TestDataSeeder.INSTITUTION_ID)
                .teacherId(UUID.randomUUID())
                .title("Integrity class " + UUID.randomUUID().toString().substring(0, 8))
                .scheduledAt(LocalDateTime.now().minusMinutes(30))
                .startedAt(LocalDateTime.now().minusMinutes(30))
                .durationMinutes(minutes)
                .status("IN_PROGRESS")
                .isDeleted(false)
                .build());
    }

    private LiveClassParticipant participant(LocalDateTime joined, LocalDateTime left) {
        LiveClassParticipant p = new LiveClassParticipant();
        p.setJoinedAt(joined);
        p.setLeftAt(left);
        return p;
    }
}