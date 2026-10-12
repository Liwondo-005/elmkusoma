package tz.elmkusoma.security;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tz.elmkusoma.course.domain.LiveClass;
import tz.elmkusoma.course.repository.LiveClassRepository;
import tz.elmkusoma.event.domain.Replay;
import tz.elmkusoma.event.repository.ReplayRepository;
import tz.elmkusoma.liveclass.domain.LiveClassParticipant;
import tz.elmkusoma.liveclass.repository.LiveClassParticipantRepository;
import tz.elmkusoma.testutil.TestDataSeeder;
import tz.elmkusoma.testutil.TestTokens;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Authorisation on the recording <em>byte</em> path.
 *
 * <p>Every other recording endpoint returns metadata; this suite covers the one that serves
 * the file. A copied URL is worthless here, because the URL is only a pointer at an endpoint
 * that re-checks role, institution, class ownership and session participation on every
 * request - including every Range request a player makes while seeking.</p>
 *
 * <p>Storage is a real local HTTP server, not a mock, so the 200s and 206s below are real
 * bytes travelling the real proxy path. That establishes the authorisation and proxying
 * behaviour. It establishes <em>nothing</em> about Backblaze B2: reachability, application
 * key permissions, path-style addressing and the actual LiveKit {@code location} scheme all
 * still need a real recording in an authorized environment.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class RecordingPlaybackAuthorizationTest {

    private static final String BUCKET = "playback-tests";
    private static final byte[] LESSON_BYTES =
            "0123456789abcdefghij".getBytes(StandardCharsets.UTF_8);

    private static final AtomicInteger hostileHits = new AtomicInteger();

    private static HttpServer storage;
    private static HttpServer hostile;

    static {
        try {
            storage = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            storage.createContext("/", exchange -> {
                String path = exchange.getRequestURI().getPath();
                if (!path.startsWith("/" + BUCKET + "/")) {
                    exchange.sendResponseHeaders(404, -1);
                    exchange.close();
                    return;
                }
                exchange.getResponseHeaders().add("Content-Type", "video/mp4");
                String range = exchange.getRequestHeaders().getFirst("Range");
                if (range != null && range.matches("bytes=\\d*-\\d*")) {
                    int end = Math.min(Integer.parseInt(range.substring("bytes=".length()).split("-", -1)[1]),
                            LESSON_BYTES.length - 1);
                    byte[] slice = java.util.Arrays.copyOfRange(LESSON_BYTES, 0, end + 1);
                    exchange.getResponseHeaders().add("Content-Range",
                            "bytes 0-" + end + "/" + LESSON_BYTES.length);
                    exchange.sendResponseHeaders(206, slice.length);
                    try (OutputStream out = exchange.getResponseBody()) {
                        out.write(slice);
                    }
                    return;
                }
                exchange.sendResponseHeaders(200, LESSON_BYTES.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(LESSON_BYTES);
                }
            });
            storage.start();

            // A second storage host on a different port. The allow-list is scheme + host +
            // port, so anything persisted against this one must never be fetched.
            hostile = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            hostile.createContext("/", exchange -> {
                hostileHits.incrementAndGet();
                exchange.sendResponseHeaders(200, -1);
                exchange.close();
            });
            hostile.start();
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @DynamicPropertySource
    static void recordingStorage(DynamicPropertyRegistry registry) {
        registry.add("livekit.egress.enabled", () -> "true");
        registry.add("livekit.egress.output-bucket", () -> BUCKET);
        registry.add("livekit.egress.output-key", () -> "k");
        registry.add("livekit.egress.secret", () -> "s");
        registry.add("livekit.egress.region", () -> "eu-central-005");
        registry.add("livekit.egress.endpoint",
                () -> "http://127.0.0.1:" + storage.getAddress().getPort());
        registry.add("app.frontend-url", () -> "http://localhost:3000");
    }

    @AfterAll
    static void stopServers() {
        if (storage != null) {
            storage.stop(0);
        }
        if (hostile != null) {
            hostile.stop(0);
        }
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private LiveClassRepository liveClassRepository;
    @Autowired
    private ReplayRepository replayRepository;
    @Autowired
    private LiveClassParticipantRepository participantRepository;

    // â”€â”€ fixtures â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private LiveClass completedClass() {
        return liveClassRepository.save(LiveClass.builder()
                .institutionId(TestDataSeeder.INSTITUTION_ID)
                .teacherId(TestDataSeeder.TEACHER_ID)
                .title("Playback class " + run())
                .scheduledAt(LocalDateTime.now().minusHours(2))
                .durationMinutes(60)
                .status("COMPLETED")
                .isDeleted(false)
                .build());
    }

    private Replay replayFor(LiveClass lc, String objectKey) {
        return replayRepository.save(Replay.builder()
                .liveSessionId(lc.getId())
                // Same institution as the session it belongs to. Pinning it to the caller's
                // institution would make every "foreign" fixture look local.
                .institutionId(lc.getInstitutionId())
                .title("Playback replay " + run())
                .status(Replay.STATUS_AVAILABLE)
                .recordingUrl(objectKey)
                .viewCount(0)
                .lastPositionSeconds(0)
                .build());
    }

    private void joinAsStudent(LiveClass lc) {
        participantRepository.save(LiveClassParticipant.builder()
                .liveClassId(lc.getId())
                .userId(TestDataSeeder.STUDENT_USER_ID)
                .role("LEARNER")
                .joinedAt(LocalDateTime.now().minusHours(1))
                .isDeleted(false)
                .build());
    }

    private static String run() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private static String objectKey(String tag) {
        return "recordings/" + tag + "/" + UUID.randomUUID() + ".mp4";
    }

    // â”€â”€ an authorised participant can play â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    void aStudentWhoTookPartInTheSessionCanStreamTheRecording() throws Exception {
        LiveClass lc = completedClass();
        joinAsStudent(lc);
        Replay replay = replayFor(lc, objectKey("entitled"));

        mockMvc.perform(get("/v1/replays/" + replay.getId() + "/playback")
                        .header("X-Institution-Id", TestDataSeeder.INSTITUTION_ID.toString())
                        .header("Authorization", "Bearer " + TestTokens.studentToken()))
                .andExpect(status().isOk())
                .andExpect(content().string(new String(LESSON_BYTES, StandardCharsets.UTF_8)));
    }

    @Test
    void theAuthorizedViewerCanSeekWithRangeRequests() throws Exception {
        LiveClass lc = completedClass();
        joinAsStudent(lc);
        Replay replay = replayFor(lc, objectKey("seeking"));

        mockMvc.perform(get("/v1/replays/" + replay.getId() + "/playback")
                        .header("X-Institution-Id", TestDataSeeder.INSTITUTION_ID.toString())
                        .header("Authorization", "Bearer " + TestTokens.studentToken())
                        .header("Range", "bytes=0-4"))
                .andExpect(status().isPartialContent())
                .andExpect(content().string("01234"));
    }

    @Test
    void anAdminCanStreamAnyRecordingInScope() throws Exception {
        LiveClass lc = completedClass();
        Replay replay = replayFor(lc, objectKey("admin"));

        mockMvc.perform(get("/v1/replays/" + replay.getId() + "/playback")
                        .header("X-Institution-Id", TestDataSeeder.INSTITUTION_ID.toString())
                        .header("Authorization", "Bearer " + TestTokens.adminToken()))
                .andExpect(status().isOk());
    }

    // â”€â”€ denied without entitlement â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    void aStudentWhoDidNotTakePartIsDenied() throws Exception {
        LiveClass lc = completedClass();
        Replay replay = replayFor(lc, objectKey("not-entitled"));

        mockMvc.perform(get("/v1/replays/" + replay.getId() + "/playback")
                        .header("X-Institution-Id", TestDataSeeder.INSTITUTION_ID.toString())
                        .header("Authorization", "Bearer " + TestTokens.studentToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void anUnauthenticatedRequestIsDeniedAndNoBytesLeaveTheServer() throws Exception {
        LiveClass lc = completedClass();
        joinAsStudent(lc);
        Replay replay = replayFor(lc, objectKey("anonymous"));

        MvcResult result = mockMvc.perform(get("/v1/replays/" + replay.getId() + "/playback")
                        .header("X-Institution-Id", TestDataSeeder.INSTITUTION_ID.toString()))
                .andExpect(status().isForbidden())
                .andReturn();

        assertEquals("", result.getResponse().getContentAsString(StandardCharsets.UTF_8),
                "a denial must not carry any recording bytes or infrastructure detail");
    }

    @Test
    void aCopiedPlaybackUrlIsWorthlessWithoutTheCallersOwnSession() throws Exception {
        LiveClass lc = completedClass();
        joinAsStudent(lc);
        Replay replay = replayFor(lc, objectKey("copied"));
        String copiedUrl = "/v1/replays/" + replay.getId() + "/playback";

        // The copied pointer alone grants nothing...
        mockMvc.perform(get(copiedUrl)
                        .header("X-Institution-Id", TestDataSeeder.INSTITUTION_ID.toString()))
                .andExpect(status().isForbidden());

        // ...and it does not carry anyone else's entitlement with it either: a different
        // student of the same institution still has to satisfy the participation rule.
        mockMvc.perform(get(copiedUrl)
                        .header("X-Institution-Id", TestDataSeeder.INSTITUTION_ID.toString())
                        .header("Authorization", "Bearer " + TestTokens.otherStudentToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void crossInstitutionReplayIdsAreIndistinguishableFromUnknownOnes() throws Exception {
        LiveClass foreign = liveClassRepository.save(LiveClass.builder()
                .institutionId(UUID.randomUUID())
                .teacherId(TestDataSeeder.TEACHER_ID)
                .title("Foreign playback " + run())
                .scheduledAt(LocalDateTime.now().minusHours(2))
                .durationMinutes(60)
                .status("COMPLETED")
                .isDeleted(false)
                .build());
        Replay foreignReplay = replayFor(foreign, objectKey("foreign"));

        mockMvc.perform(get("/v1/replays/" + foreignReplay.getId() + "/playback")
                        .header("X-Institution-Id", TestDataSeeder.INSTITUTION_ID.toString())
                        .header("Authorization", "Bearer " + TestTokens.studentToken()))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/v1/replays/" + UUID.randomUUID() + "/playback")
                        .header("X-Institution-Id", TestDataSeeder.INSTITUTION_ID.toString())
                        .header("Authorization", "Bearer " + TestTokens.studentToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void swappingTheReplayIdCannotReachAnotherClassesRecording() throws Exception {
        LiveClass first = completedClass();
        joinAsStudent(first);
        Replay entitled = replayFor(first, objectKey("class-a"));

        LiveClass second = completedClass();
        Replay foreignToCaller = replayFor(second, objectKey("class-b"));

        // The entitled call streams class A's object...
        mockMvc.perform(get("/v1/replays/" + entitled.getId() + "/playback")
                        .header("X-Institution-Id", TestDataSeeder.INSTITUTION_ID.toString())
                        .header("Authorization", "Bearer " + TestTokens.studentToken()))
                .andExpect(status().isOk());

        // ...and re-using that same pointer against class B's replay id is still decided by
        // class B's entitlement, not by anything the caller supplied.
        mockMvc.perform(get("/v1/replays/" + foreignToCaller.getId() + "/playback")
                        .header("X-Institution-Id", TestDataSeeder.INSTITUTION_ID.toString())
                        .header("Authorization", "Bearer " + TestTokens.studentToken()))
                .andExpect(status().isForbidden());
    }

    // â”€â”€ authentication transport â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    void theBrowsersAccessTokenCookieAuthenticatesThePlaybackRequest() throws Exception {
        // <video src> cannot carry an Authorization header. The application already mirrors
        // the access token into an elmkusoma_access_token cookie, so the byte request is
        // still authenticated by the same JWT - not by an unauthenticated bypass.
        LiveClass lc = completedClass();
        joinAsStudent(lc);
        Replay replay = replayFor(lc, objectKey("cookie"));

        mockMvc.perform(get("/v1/replays/" + replay.getId() + "/playback")
                        .header("X-Institution-Id", TestDataSeeder.INSTITUTION_ID.toString())
                        .cookie(new jakarta.servlet.http.Cookie("elmkusoma_access_token",
                                TestTokens.studentToken())))
                .andExpect(status().isOk())
                .andExpect(content().string(new String(LESSON_BYTES, StandardCharsets.UTF_8)));
    }

    @Test
    void theCookieFallbackIsScopedToPlaybackAndDoesNotAuthenticateOtherEndpoints() throws Exception {
        // The whole point of scoping: no other endpoint's authentication changes.
        LiveClass lc = completedClass();
        joinAsStudent(lc);
        Replay replay = replayFor(lc, objectKey("scoped"));

        mockMvc.perform(get("/v1/replays/" + replay.getId())
                        .header("X-Institution-Id", TestDataSeeder.INSTITUTION_ID.toString())
                        .cookie(new jakarta.servlet.http.Cookie("elmkusoma_access_token",
                                TestTokens.studentToken())))
                .andExpect(status().isForbidden());

        // ...while the same cookie does authenticate the byte request it exists for.
        mockMvc.perform(get("/v1/replays/" + replay.getId() + "/playback")
                        .header("X-Institution-Id", TestDataSeeder.INSTITUTION_ID.toString())
                        .cookie(new jakarta.servlet.http.Cookie("elmkusoma_access_token",
                                TestTokens.studentToken())))
                .andExpect(status().isOk());
    }

    @Test
    void anExpiredOrForgedCookieIsRejected() throws Exception {
        LiveClass lc = completedClass();
        joinAsStudent(lc);
        Replay replay = replayFor(lc, objectKey("forged"));

        mockMvc.perform(get("/v1/replays/" + replay.getId() + "/playback")
                        .header("X-Institution-Id", TestDataSeeder.INSTITUTION_ID.toString())
                        .cookie(new jakarta.servlet.http.Cookie("elmkusoma_access_token",
                                "not-a-jwt")))
                .andExpect(status().isForbidden());
    }

    // â”€â”€ untrusted stored locations never become outbound requests â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    void aStoredLocationPointingAtAnotherStorageHostIsRefusedAndNeverFetched() throws Exception {
        LiveClass lc = completedClass();
        joinAsStudent(lc);
        String hostileLocation = "http://127.0.0.1:" + hostile.getAddress().getPort()
                + "/" + BUCKET + "/recordings/secret.mp4";
        Replay replay = replayFor(lc, hostileLocation);

        int before = hostileHits.get();
        mockMvc.perform(get("/v1/replays/" + replay.getId() + "/playback")
                        .header("X-Institution-Id", TestDataSeeder.INSTITUTION_ID.toString())
                        .header("Authorization", "Bearer " + TestTokens.studentToken()))
                .andExpect(status().isServiceUnavailable());

        assertEquals(before, hostileHits.get(),
                "a stored location outside the configured endpoint must never be fetched");
    }

    @Test
    void anArbitraryUrlStoredAsALocationIsRefused() throws Exception {
        LiveClass lc = completedClass();
        joinAsStudent(lc);
        Replay replay = replayFor(lc, "https://attacker.example.com/exfiltrate?to=evil");

        mockMvc.perform(get("/v1/replays/" + replay.getId() + "/playback")
                        .header("X-Institution-Id", TestDataSeeder.INSTITUTION_ID.toString())
                        .header("Authorization", "Bearer " + TestTokens.studentToken()))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void aReplayWithNoLocationIsA404NotAFabricatedUrl() throws Exception {
        LiveClass lc = completedClass();
        joinAsStudent(lc);
        Replay replay = replayRepository.save(Replay.builder()
                .liveSessionId(lc.getId())
                .institutionId(TestDataSeeder.INSTITUTION_ID)
                .title("No location " + run())
                .status(Replay.STATUS_PROCESSING)
                .viewCount(0)
                .lastPositionSeconds(0)
                .build());

        mockMvc.perform(get("/v1/replays/" + replay.getId() + "/playback")
                        .header("X-Institution-Id", TestDataSeeder.INSTITUTION_ID.toString())
                        .header("Authorization", "Bearer " + TestTokens.studentToken()))
                .andExpect(status().isNotFound());
    }

    // â”€â”€ the storage location is never disclosed â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    void replayResponsesExposeThePlaybackUrlAndNeverTheStorageLocation() throws Exception {
        LiveClass lc = completedClass();
        joinAsStudent(lc);
        String key = objectKey("disclosure");
        Replay replay = replayFor(lc, key);

        String detail = mockMvc.perform(get("/v1/replays/" + replay.getId())
                        .header("X-Institution-Id", TestDataSeeder.INSTITUTION_ID.toString())
                        .header("Authorization", "Bearer " + TestTokens.studentToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.replay.videoUrl")
                        .value("http://localhost:3000/v1/replays/" + replay.getId() + "/playback"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertFalse(detail.contains("127.0.0.1"),
                "the storage host must never appear in a client response: " + detail);
        assertFalse(detail.contains(BUCKET),
                "the bucket name must never appear in a client response");
        assertFalse(detail.contains(key),
                "the object key must never appear in a client response");
    }

    @Test
    void aLiveClassResponseExposesThePlaybackUrlAndNeverTheStorageLocation() throws Exception {
        LiveClass lc = liveClassRepository.save(LiveClass.builder()
                .institutionId(TestDataSeeder.INSTITUTION_ID)
                .teacherId(TestDataSeeder.TEACHER_ID)
                .title("Recorded live class " + run())
                .scheduledAt(LocalDateTime.now().minusHours(2))
                .durationMinutes(60)
                .status("COMPLETED")
                .recordingUrl("recordings/" + objectKey("liveclass"))
                .isDeleted(false)
                .build());

        String body = mockMvc.perform(get("/v1/learner/live-classes/" + lc.getId())
                        .header("X-Institution-Id", TestDataSeeder.INSTITUTION_ID.toString())
                        .header("Authorization", "Bearer " + TestTokens.studentToken()))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertTrue(body.contains("/v1/live-session/classes/" + lc.getId() + "/recording/playback"),
                "the response must carry the entitlement-checked playback URL, got: " + body);
        assertFalse(body.contains("127.0.0.1"),
                "the storage host must never appear in a client response");
        assertFalse(body.contains(BUCKET),
                "the bucket name must never appear in a client response");
    }
}
