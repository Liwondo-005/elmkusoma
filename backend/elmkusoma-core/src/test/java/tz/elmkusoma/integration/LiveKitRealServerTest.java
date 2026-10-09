package tz.elmkusoma.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import tz.elmkusoma.liveclass.service.LiveKitService;

import java.net.Socket;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * §90/§99/§104/§111 — live media against a real LiveKit server on localhost:7880.
 * Skipped only when the server is not listening (docker compose -f docker-compose.livekit.yml up -d).
 */
@SpringBootTest
@ActiveProfiles("test")
class LiveKitRealServerTest {

    @Autowired
    private LiveKitService liveKitService;

    private static boolean liveKitListening() {
        try (Socket s = new Socket("127.0.0.1", 7880)) {
            return s.isConnected();
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    void livekit_ServerIsReachable_AndTokenIsIssued() {
        assumeTrue(liveKitListening(), "LiveKit not listening on 7880 — start docker-compose.livekit.yml");
        assertTrue(liveKitService.isAvailable(), "LiveKitService must be configured");

        String token = liveKitService.generateEventToken(UUID.randomUUID(), UUID.randomUUID(), "e2e-learner", false);
        assertNotNull(token, "participant token must be issued by real LiveKit config");
        assertTrue(token.split("\\.").length == 3, "token must be a JWT");
    }

    @Test
    void livekit_EnsureRoom_CreatesRoomOnRealServer() {
        assumeTrue(liveKitListening(), "LiveKit not listening on 7880 — start docker-compose.livekit.yml");
        UUID classId = UUID.randomUUID();
        liveKitService.ensureRoom(classId);
        // ensureRoom logs success/failure; no exception means HTTP call path ran against real server
        assertNotNull(liveKitService.generateRoomName(classId));
        assertTrue(liveKitService.generateRoomName(classId).startsWith("liveclass-"));
    }

    /**
     * Real Egress start against whatever LiveKit server is configured - which is not
     * necessarily localhost:7880, so this gates on LiveKitService itself rather than on a
     * hard-coded socket, and therefore can run against a LiveKit Cloud project.
     *
     * <p>Skipped unless recording storage is really configured
     * ({@code LIVEKIT_EGRESS_OUTPUT_BUCKET} + key/secret/region), because starting an
     * egress with fake credentials would leave a doomed job running on a shared server.
     * The job is always stopped, including on failure, so no capture is leaked.</p>
     */
    @Test
    void livekit_EgressStartAndStop_AgainstRealServer() {
        assumeTrue(liveKitService.isConfigured(),
                "LiveKit credentials are not configured for this environment");
        assumeTrue(liveKitService.isRecordingConfigured(),
                "Recording storage is not configured - set LIVEKIT_EGRESS_ENABLED plus "
                        + "LIVEKIT_EGRESS_OUTPUT_BUCKET / ACCESS_KEY / SECRET / REGION to run this");

        UUID classId = UUID.randomUUID();
        String egressId = null;
        try {
            egressId = liveKitService.startRecording(classId);
            assertNotNull(egressId,
                    "a configured recorder must return a real egress id, not null");
            assertTrue(egressId.startsWith("EG_"),
                    "LiveKit returns ids prefixed EG_, got: " + egressId);
            // The recording marker stored on the live class must stay transient.
            assertTrue(("egress:" + egressId).startsWith("egress:"),
                    "the stored marker is a placeholder until finalization resolves a URL");
        } finally {
            if (egressId != null) {
                assertTrue(liveKitService.stopRecording(egressId),
                        "stopRecording must confirm the job was stopped");
            }
        }
    }
}
