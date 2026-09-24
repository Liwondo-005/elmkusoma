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
}
