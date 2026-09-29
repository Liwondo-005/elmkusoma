package tz.elmkusoma.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tz.elmkusoma.testutil.TestDataSeeder;
import tz.elmkusoma.testutil.TestTokens;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Live streaming enhancement coverage:
 * 1. Broadcast source model (broadcast_source) round trip through the existing
 *    teacher live-class API — including the backward-compatible default for
 *    clients that never send a source.
 * 2. External ingest endpoints (OBS/encoder/studio → existing LiveKit room):
 *    role gates, cross-institution denial, and the HONEST not-configured state
 *    (application-test.properties keeps livekit.ingress.enabled=false, so the
 *    API must report configured=false / 503 instead of inventing RTMP/WHIP/SRT
 *    endpoints).
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class LiveBroadcastSourceIngressTest {

    private static final String INSTITUTION = "00000000-0000-0000-0000-000000000001";
    private static final String SEEDED_CLASS = TestDataSeeder.CLASS_ID.toString();

    private static final String TITLE_EXTERNAL = "Source Model External Camera Test";
    private static final String TITLE_DEFAULT = "Source Model Default Test";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ------------------------------------------------------------------
    // Ingest (WHIP/RTMP/SRT) endpoints — authorization + honest states
    // ------------------------------------------------------------------

    @Test
    void ingress_learnerRole_returnsForbidden() throws Exception {
        mockMvc.perform(get("/v1/live-session/classes/" + SEEDED_CLASS + "/ingress")
                        .header("Authorization", "Bearer " + TestTokens.studentToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void ingress_unauthenticated_returnsClientError() throws Exception {
        mockMvc.perform(get("/v1/live-session/classes/" + SEEDED_CLASS + "/ingress"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void ingress_crossInstitution_returnsForbidden() throws Exception {
        // An admin whose X-Institution-Id points at a different tenant must be
        // denied without leaking whether the class exists.
        mockMvc.perform(get("/v1/live-session/classes/" + SEEDED_CLASS + "/ingress")
                        .header("Authorization", "Bearer " + TestTokens.adminToken())
                        .header("X-Institution-Id", "11111111-1111-1111-1111-111111111111"))
                .andExpect(status().isForbidden());
    }

    @Test
    void ingress_get_whenNotConfigured_returnsHonestNotConfiguredState() throws Exception {
        mockMvc.perform(get("/v1/live-session/classes/" + SEEDED_CLASS + "/ingress")
                        .header("Authorization", "Bearer " + TestTokens.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.configured").value(false))
                .andExpect(jsonPath("$.data.ingresses").isEmpty())
                .andExpect(jsonPath("$.data.message", containsString("not configured")));
    }

    @Test
    void ingress_create_whenNotConfigured_returns503WithExplanation() throws Exception {
        mockMvc.perform(post("/v1/live-session/classes/" + SEEDED_CLASS + "/ingress")
                        .header("Authorization", "Bearer " + TestTokens.adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"protocol\":\"WHIP\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error", containsString("not configured")));
    }

    @Test
    void ingress_delete_whenNotConfigured_removesNothingAndSucceeds() throws Exception {
        mockMvc.perform(delete("/v1/live-session/classes/" + SEEDED_CLASS + "/ingress")
                        .header("Authorization", "Bearer " + TestTokens.adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.configured").value(false))
                .andExpect(jsonPath("$.data.removed").value(0));
    }

    // ------------------------------------------------------------------
    // Broadcast source model — create / read / validation round trip
    // ------------------------------------------------------------------

    @Test
    void broadcastSource_roundTrip_echoesExternalSourceAndDefaultsToBrowser() throws Exception {
        cancelLeftovers(TITLE_EXTERNAL);
        cancelLeftovers(TITLE_DEFAULT);

        String externalId = null;
        String defaultId = null;
        try {
            MvcResult external = mockMvc.perform(post("/v1/teachers/me/live-classes")
                            .header("Authorization", "Bearer " + TestTokens.teacherToken())
                            .header("X-Institution-Id", INSTITUTION)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"" + TITLE_EXTERNAL + "\","
                                    + "\"scheduledAt\":\"" + LocalDateTime.now().plusDays(30) + "\","
                                    + "\"durationMinutes\":15,\"broadcastSource\":\"USB_CAMERA\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.broadcastSource").value("USB_CAMERA"))
                    .andReturn();
            externalId = readData(external).get("id").asText();

            MvcResult fallback = mockMvc.perform(post("/v1/teachers/me/live-classes")
                            .header("Authorization", "Bearer " + TestTokens.teacherToken())
                            .header("X-Institution-Id", INSTITUTION)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"" + TITLE_DEFAULT + "\","
                                    + "\"scheduledAt\":\"" + LocalDateTime.now().plusDays(30).plusHours(4) + "\","
                                    + "\"durationMinutes\":15}"))
                    .andExpect(status().isCreated())
                    // Older clients never send a source → BROWSER, exactly the pre-change default.
                    .andExpect(jsonPath("$.data.broadcastSource").value("BROWSER"))
                    .andReturn();
            defaultId = readData(fallback).get("id").asText();

            // Read back through the existing GET used by the Live Control Room.
            mockMvc.perform(get("/v1/teachers/me/live-classes/" + externalId)
                            .header("Authorization", "Bearer " + TestTokens.teacherToken())
                            .header("X-Institution-Id", INSTITUTION))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.broadcastSource").value("USB_CAMERA"));
        } finally {
            cancelQuietly(externalId);
            cancelQuietly(defaultId);
        }
    }

    @Test
    void broadcastSource_invalidValue_returns400ListingSupportedSources() throws Exception {
        mockMvc.perform(post("/v1/teachers/me/live-classes")
                        .header("Authorization", "Bearer " + TestTokens.teacherToken())
                        .header("X-Institution-Id", INSTITUTION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Bad Source Test\","
                                + "\"scheduledAt\":\"" + LocalDateTime.now().plusDays(30).plusHours(8) + "\","
                                + "\"durationMinutes\":15,\"broadcastSource\":\"TOASTER\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("USB_CAMERA")));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private JsonNode readData(MvcResult result) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.get("data");
    }

    /** Cancels any leftover class with the same title so reruns never hit a schedule conflict. */
    private void cancelLeftovers(String title) {
        try {
            MvcResult list = mockMvc.perform(get("/v1/teachers/me/live-classes")
                            .header("Authorization", "Bearer " + TestTokens.teacherToken())
                            .header("X-Institution-Id", INSTITUTION))
                    .andExpect(status().isOk())
                    .andReturn();
            JsonNode data = objectMapper.readTree(list.getResponse().getContentAsString()).get("data");
            if (data == null || !data.isArray()) {
                return;
            }
            for (JsonNode item : data) {
                if (title.equals(item.path("title").asText()) && !item.path("status").asText().equals("CANCELLED")) {
                    cancelQuietly(item.get("id").asText());
                }
            }
        } catch (Exception e) {
            // pre-cleanup must never mask the actual assertion below
        }
    }

    private void cancelQuietly(String classId) {
        if (classId == null || classId.isBlank()) {
            return;
        }
        try {
            mockMvc.perform(delete("/v1/teachers/me/live-classes/" + classId)
                            .header("Authorization", "Bearer " + TestTokens.teacherToken())
                            .header("X-Institution-Id", INSTITUTION))
                    .andExpect(status().isOk());
        } catch (Exception e) {
            // cleanup must never mask the actual test failure
        }
    }
}
