package tz.elmkusoma.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tz.elmkusoma.testutil.TestDataSeeder;
import tz.elmkusoma.testutil.TestTokens;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Host continuity contract for the live classroom:
 * <pre>
 *   schedule -> start -> (host state) -> join -> ... -> end
 * </pre>
 * <ul>
 *   <li>A host reads the SAME session state back from {@code /host-state} — status,
 *       startedAt, room name and recording state come from the server, never from
 *       browser state, so a reload or navigation away cannot invent a new session.</li>
 *   <li>{@code roomName} is derived from the class id, which is what makes
 *       "host leaves and returns" rejoin the same LiveKit room.</li>
 *   <li>Only the host (or an admin) may read host state or end the session.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class LiveHostContinuityTest {

    @Autowired
    private MockMvc mockMvc;

    /** Distinct, non-overlapping windows so the seeded teacher's conflict check never fires. */
    private static final java.util.concurrent.atomic.AtomicInteger SLOT =
            new java.util.concurrent.atomic.AtomicInteger();

    private MvcResult createAndStart() throws Exception {
        String run = java.util.UUID.randomUUID().toString().substring(0, 8);
        int slot = SLOT.getAndIncrement();
        MvcResult created = mockMvc.perform(post("/v1/teachers/me/live-classes")
                        .header("X-Institution-Id", TestDataSeeder.INSTITUTION_ID.toString())
                        .header("Authorization", "Bearer " + TestTokens.teacherToken())
                        .contentType("application/json")
                        // Creation rejects past dates (1-minute grace), so schedule slightly ahead and then
                // start explicitly — that is the real host flow.
                .content("{\"title\":\"Host continuity " + run + "\","
                                + "\"scheduledAt\":\"" + java.time.LocalDateTime.now()
                                        .plusMinutes(30 + slot * 180L).withNano(0)
                                        + "\",\"durationMinutes\":30}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        String classId = com.jayway.jsonpath.JsonPath.read(
                created.getResponse().getContentAsString(), "$.data.id");
        mockMvc.perform(post("/v1/teachers/me/live-classes/" + classId + "/start")
                        .header("X-Institution-Id", TestDataSeeder.INSTITUTION_ID.toString())
                        .header("Authorization", "Bearer " + TestTokens.teacherToken()))
                .andExpect(status().isOk());
        return created;
    }

    @Test
    void hostCanReadHostStateOfOwnActiveSession() throws Exception {
        MvcResult created = createAndStart();
        String classId = com.jayway.jsonpath.JsonPath.read(
                created.getResponse().getContentAsString(), "$.data.id");

        MvcResult state = mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .get("/v1/live-session/classes/" + classId + "/host-state")
                        .header("X-Institution-Id", TestDataSeeder.INSTITUTION_ID.toString())
                        .header("Authorization", "Bearer " + TestTokens.teacherToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.active").value(true))
                .andExpect(jsonPath("$.data.canEndSession").value(true))
                .andReturn();

        String body = state.getResponse().getContentAsString();
        assertTrue(body.contains("liveclass-" + classId),
                "host-state must expose the deterministic LiveKit room name for this class");
        // Recording state is reported from the server so a re-entering host never guesses.
        // recordingActive must always be present; the egress id only exists while recording.
        assertTrue(body.contains("\"recordingActive\""), "recordingActive must always be reported");
        assertTrue(body.contains("\"recordingAvailable\""), "recordingAvailable must always be reported");
        assertEquals(false, com.jayway.jsonpath.JsonPath.read(body, "$.data.recordingActive"),
                "no recording is running for this freshly started class");
    }

    @Test
    void hostStateIsStableAcrossRepeatedReads_soReturningHostResumesSameSession() throws Exception {
        MvcResult created = createAndStart();
        String classId = com.jayway.jsonpath.JsonPath.read(
                created.getResponse().getContentAsString(), "$.data.id");

        String first = mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .get("/v1/live-session/classes/" + classId + "/host-state")
                        .header("X-Institution-Id", TestDataSeeder.INSTITUTION_ID.toString())
                        .header("Authorization", "Bearer " + TestTokens.teacherToken()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String second = mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .get("/v1/live-session/classes/" + classId + "/host-state")
                        .header("X-Institution-Id", TestDataSeeder.INSTITUTION_ID.toString())
                        .header("Authorization", "Bearer " + TestTokens.teacherToken()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String startedAt1 = com.jayway.jsonpath.JsonPath.read(first, "$.data.startedAt");
        String startedAt2 = com.jayway.jsonpath.JsonPath.read(second, "$.data.startedAt");
        String room1 = com.jayway.jsonpath.JsonPath.read(first, "$.data.roomName");
        String room2 = com.jayway.jsonpath.JsonPath.read(second, "$.data.roomName");

        assertEquals(startedAt1, startedAt2, "startedAt must not move between reads of one session");
        assertEquals(room1, room2, "room name must be stable so the host rejoins the same room");
    }

    @Test
    void learnerCannotReadHostState() throws Exception {
        MvcResult created = createAndStart();
        String classId = com.jayway.jsonpath.JsonPath.read(
                created.getResponse().getContentAsString(), "$.data.id");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/v1/live-session/classes/" + classId + "/host-state")
                        .header("X-Institution-Id", TestDataSeeder.INSTITUTION_ID.toString())
                        .header("Authorization", "Bearer " + TestTokens.studentToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void endedSessionReportsInactiveAndNotEndable() throws Exception {
        MvcResult created = createAndStart();
        String classId = com.jayway.jsonpath.JsonPath.read(
                created.getResponse().getContentAsString(), "$.data.id");

        mockMvc.perform(post("/v1/teachers/me/live-classes/" + classId + "/end")
                        .header("X-Institution-Id", TestDataSeeder.INSTITUTION_ID.toString())
                        .header("Authorization", "Bearer " + TestTokens.teacherToken()))
                .andExpect(status().isOk());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/v1/live-session/classes/" + classId + "/host-state")
                        .header("X-Institution-Id", TestDataSeeder.INSTITUTION_ID.toString())
                        .header("Authorization", "Bearer " + TestTokens.teacherToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false))
                .andExpect(jsonPath("$.data.canEndSession").value(false))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }
}