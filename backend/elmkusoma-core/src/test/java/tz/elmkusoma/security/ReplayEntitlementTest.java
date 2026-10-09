package tz.elmkusoma.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tz.elmkusoma.course.domain.LiveClass;
import tz.elmkusoma.course.repository.LiveClassRepository;
import tz.elmkusoma.event.domain.Event;
import tz.elmkusoma.event.domain.EventRegistration;
import tz.elmkusoma.event.domain.Replay;
import tz.elmkusoma.event.repository.EventRegistrationRepository;
import tz.elmkusoma.event.repository.EventRepository;
import tz.elmkusoma.event.repository.ReplayRepository;
import tz.elmkusoma.liveclass.domain.LiveClassParticipant;
import tz.elmkusoma.liveclass.repository.LiveClassParticipantRepository;
import tz.elmkusoma.testutil.TestDataSeeder;
import tz.elmkusoma.testutil.TestTokens;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Replay entitlement: a learner must have taken part in the recorded session to open its
 * replay. Institution membership alone must not expose recordings.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class ReplayEntitlementTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LiveClassRepository liveClassRepository;

    @Autowired
    private LiveClassParticipantRepository participantRepository;

    @Autowired
    private ReplayRepository replayRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventRegistrationRepository registrationRepository;

    @Test
    void learnerWithNoParticipantRowCannotOpenLiveClassReplay() throws Exception {
        LiveClass lc = liveClass();
        Replay replay = replayRepository.save(Replay.builder()
                .liveSessionId(lc.getId())
                .institutionId(TestDataSeeder.INSTITUTION_ID)
                .title("Entitlement " + UUID.randomUUID().toString().substring(0, 8))
                .status(Replay.STATUS_AVAILABLE)
                .recordingUrl("https://cdn.test/entitlement.mp4")
                .viewCount(0)
                .lastPositionSeconds(0)
                .build());

        mockMvc.perform(get("/v1/learner/replays/" + replay.getId())
                        .header("X-Institution-Id", TestDataSeeder.INSTITUTION_ID.toString())
                        .header("Authorization", "Bearer " + TestTokens.studentToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void learnerWhoParticipatedCanOpenLiveClassReplay() throws Exception {
        LiveClass lc = liveClass();
        participantRepository.save(LiveClassParticipant.builder()
                .liveClassId(lc.getId())
                .userId(TestDataSeeder.STUDENT_USER_ID)
                .role("LEARNER")
                .joinedAt(LocalDateTime.now().minusMinutes(20))
                .isDeleted(false)
                .build());

        Replay replay = replayRepository.save(Replay.builder()
                .liveSessionId(lc.getId())
                .institutionId(TestDataSeeder.INSTITUTION_ID)
                .title("Entitled " + UUID.randomUUID().toString().substring(0, 8))
                .status(Replay.STATUS_AVAILABLE)
                .recordingUrl("https://cdn.test/entitled.mp4")
                .viewCount(0)
                .lastPositionSeconds(0)
                .build());

        mockMvc.perform(get("/v1/learner/replays/" + replay.getId())
                        .header("X-Institution-Id", TestDataSeeder.INSTITUTION_ID.toString())
                        .header("Authorization", "Bearer " + TestTokens.studentToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void learnerWithNoRegistrationCannotOpenEventReplay() throws Exception {
        String run = UUID.randomUUID().toString().substring(0, 8);
Event event = eventRepository.save(Event.builder()
                .institutionId(TestDataSeeder.INSTITUTION_ID)
                .organizerId(TestDataSeeder.TEACHER_USER_ID)
                .eventType("WORKSHOP")
                .title("Replay event " + run)
                .startsAt(LocalDateTime.now().minusHours(2))
                .endsAt(LocalDateTime.now().minusHours(1))
                .status("ENDED")
                .accessLevel("PUBLIC")
                // @SuperBuilder bypasses field initializers - set NOT NULL defaults explicitly
                .isFree(true)
                .requiresApproval(false)
                .build());

        Replay replay = replayRepository.save(Replay.builder()
                .eventId(event.getId())
                .institutionId(TestDataSeeder.INSTITUTION_ID)
                .title("Event replay " + run)
                .status(Replay.STATUS_AVAILABLE)
                .recordingUrl("https://cdn.test/event.mp4")
                .viewCount(0)
                .lastPositionSeconds(0)
                .build());

        mockMvc.perform(get("/v1/learner/replays/" + replay.getId())
                        .header("X-Institution-Id", TestDataSeeder.INSTITUTION_ID.toString())
                        .header("Authorization", "Bearer " + TestTokens.otherStudentToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void registeredLearnerCanOpenEventReplay() throws Exception {
        String run = UUID.randomUUID().toString().substring(0, 8);
Event event = eventRepository.save(Event.builder()
                .institutionId(TestDataSeeder.INSTITUTION_ID)
                .organizerId(TestDataSeeder.TEACHER_USER_ID)
                .eventType("WORKSHOP")
                .title("Registered replay event " + run)
                .startsAt(LocalDateTime.now().minusHours(2))
                .endsAt(LocalDateTime.now().minusHours(1))
                .status("ENDED")
                .accessLevel("PUBLIC")
                // @SuperBuilder bypasses field initializers - set NOT NULL defaults explicitly
                .isFree(true)
                .requiresApproval(false)
                .build());

        registrationRepository.save(EventRegistration.builder()
                .eventId(event.getId())
                .userId(TestDataSeeder.OTHER_STUDENT_USER_ID)
                .status("CONFIRMED")
                .registeredAt(LocalDateTime.now().minusHours(3))
                .isDeleted(false)
                .build());

        Replay replay = replayRepository.save(Replay.builder()
                .eventId(event.getId())
                .institutionId(TestDataSeeder.INSTITUTION_ID)
                .title("Registered replay " + run)
                .status(Replay.STATUS_AVAILABLE)
                .recordingUrl("https://cdn.test/registered.mp4")
                .viewCount(0)
                .lastPositionSeconds(0)
                .build());

        mockMvc.perform(get("/v1/learner/replays/" + replay.getId())
                        .header("X-Institution-Id", TestDataSeeder.INSTITUTION_ID.toString())
                        .header("Authorization", "Bearer " + TestTokens.otherStudentToken()))
                .andExpect(status().isOk());
    }

    @Test
    void replayListingHidesSessionsTheLearnerDidNotAttend() throws Exception {
        LiveClass lc = liveClass();
        replayRepository.save(Replay.builder()
                .liveSessionId(lc.getId())
                .institutionId(TestDataSeeder.INSTITUTION_ID)
                .title("Hidden from learner " + UUID.randomUUID().toString().substring(0, 8))
                .status(Replay.STATUS_AVAILABLE)
                .recordingUrl("https://cdn.test/hidden.mp4")
                .viewCount(0)
                .lastPositionSeconds(0)
                .build());

        MvcResultHolder result = new MvcResultHolder();
        String body = mockMvc.perform(get("/v1/learner/replays")
                        .header("X-Institution-Id", TestDataSeeder.INSTITUTION_ID.toString())
                        .header("Authorization", "Bearer " + TestTokens.studentToken()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertTrue(!body.contains("Hidden from learner"),
                "listing must not advertise replays the learner is not entitled to");
        assertEquals(0, result.count(body, "Hidden from learner"));
    }

    private static class MvcResultHolder {
        int count(String haystack, String needle) {
            int n = 0;
            int i = 0;
            while ((i = haystack.indexOf(needle, i)) >= 0) {
                n++;
                i += needle.length();
            }
            return n;
        }
    }

    private LiveClass liveClass() {
        return liveClassRepository.save(LiveClass.builder()
                .institutionId(TestDataSeeder.INSTITUTION_ID)
                .teacherId(UUID.randomUUID())
                .title("Entitlement class " + UUID.randomUUID().toString().substring(0, 8))
                .scheduledAt(LocalDateTime.now().minusHours(1))
                .durationMinutes(60)
                .status("COMPLETED")
                .isDeleted(false)
                .build());
    }
}