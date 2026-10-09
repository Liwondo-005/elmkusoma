package tz.elmkusoma.liveclass.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tz.elmkusoma.liveclass.config.LiveKitConfig;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Token grants must reflect the participant role, otherwise an OBSERVER (or a demoted
 * participant) can keep publishing media for the whole session.
 */
class LiveKitTokenRoleGrantTest {

    private LiveKitService service;
    private ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        LiveKitConfig config = new LiveKitConfig();
        config.setServer(serverConfig());
        config.setEgress(new LiveKitConfig.EgressConfig());
        config.setIngress(new LiveKitConfig.IngressConfig());

        service = new LiveKitService(config);
    }

    private LiveKitConfig.ServerConfig serverConfig() {
        LiveKitConfig.ServerConfig server = new LiveKitConfig.ServerConfig();
        server.setUrl("ws://localhost:7880");
        server.setApiKey("devkey");
        server.setApiSecret("devsecret-devsecret-devsecret-dev01!");
        return server;
    }

    @Test
    void teacherToken_canPublish_and_hasRoomAdmin() throws Exception {
        String jwt = service.generateToken(java.util.UUID.randomUUID(), java.util.UUID.randomUUID(),
                "teacher-1", true, "TEACHER");
        assertNotNull(jwt);
        var video = videoGrants(jwt);
        assertTrue(video.get("canPublish") instanceof Boolean b && b);
        assertTrue(video.get("roomAdmin") instanceof Boolean b && b);
        assertTrue(video.get("roomCreate") instanceof Boolean b && b);
    }

    @Test
    void learnerToken_canPublish_forInteractiveClassroom() throws Exception {
        String jwt = service.generateToken(java.util.UUID.randomUUID(), java.util.UUID.randomUUID(),
                "learner-1", false, "LEARNER");
        assertNotNull(jwt);
        var video = videoGrants(jwt);
        assertTrue(video.get("canPublish") instanceof Boolean b && b);
        assertTrue(video.get("canSubscribe") instanceof Boolean b && b);
        // Learners must never gain room administration.
        assertFalse(video.containsKey("roomAdmin"));
        assertFalse(video.containsKey("roomCreate"));
    }

    @Test
    void moderatorToken_canPublish_withoutRoomAdmin() throws Exception {
        String jwt = service.generateToken(java.util.UUID.randomUUID(), java.util.UUID.randomUUID(),
                "mod-1", false, "MODERATOR");
        assertNotNull(jwt);
        var video = videoGrants(jwt);
        assertTrue(video.get("canPublish") instanceof Boolean b && b);
        assertFalse(video.containsKey("roomAdmin"));
    }

    @Test
    void observerToken_cannotPublish() throws Exception {
        String jwt = service.generateToken(java.util.UUID.randomUUID(), java.util.UUID.randomUUID(),
                "obs-1", false, "OBSERVER");
        assertNotNull(jwt);
        var video = videoGrants(jwt);
        assertFalse(video.get("canPublish") instanceof Boolean b && b,
                "OBSERVER must not be able to publish camera/microphone");
        // Observers still receive media and can send data frames (chat/reactions/raise hand).
        assertTrue(video.get("canSubscribe") instanceof Boolean b && b);
        assertTrue(video.get("canPublishData") instanceof Boolean b && b);
    }

    @Test
    void observerRoleIsCaseInsensitive() throws Exception {
        String jwt = service.generateToken(java.util.UUID.randomUUID(), java.util.UUID.randomUUID(),
                "obs-2", false, "  observer  ");
        assertNotNull(jwt);
        assertFalse(videoGrants(jwt).get("canPublish") instanceof Boolean b && b);
    }

    @Test
    void nullRole_keepsExistingLearnerBehaviour() throws Exception {
        // Participants without a recorded role (first join) must not be locked out of
        // publishing — this is a backward-compatibility guarantee.
        String jwt = service.generateToken(java.util.UUID.randomUUID(), java.util.UUID.randomUUID(),
                "learner-2", false, null);
        assertNotNull(jwt);
        assertTrue(videoGrants(jwt).get("canPublish") instanceof Boolean b && b);
    }

    @Test
    void breakoutToken_honoursObserverRole() throws Exception {
        var classId = java.util.UUID.randomUUID();
        var roomId = java.util.UUID.randomUUID();
        String observer = service.generateBreakoutToken(classId, roomId, java.util.UUID.randomUUID(),
                "obs-3", false, "OBSERVER");
        assertNotNull(observer);
        assertFalse(videoGrants(observer).get("canPublish") instanceof Boolean b && b);
        assertEquals("breakout-" + classId + "-" + roomId, videoGrants(observer).get("room"));
    }

    @Test
    void roomNameIsDeterministic_soReturningHostRejoinsSameRoom() {
        var classId = java.util.UUID.randomUUID();
        String first = service.generateRoomName(classId);
        String second = service.generateRoomName(classId);
        assertEquals(first, second,
                "A stable room name is what makes host-leave-and-return resume the same session");
    }

    @Test
    void tokenIsBoundToTheClassRoom() throws Exception {
        var classId = java.util.UUID.randomUUID();
        String jwt = service.generateToken(classId, java.util.UUID.randomUUID(), "u", false, "LEARNER");
        assertEquals("liveclass-" + classId, videoGrants(jwt).get("room"));
    }

    @SuppressWarnings("unchecked")
    private java.util.Map<String, Object> videoGrants(String jwt) throws Exception {
        String[] parts = jwt.split("\\.");
        assertEquals(3, parts.length);
        String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]),
                java.nio.charset.StandardCharsets.UTF_8);
        java.util.Map<String, Object> claims = mapper.readValue(payloadJson, java.util.Map.class);
        return (java.util.Map<String, Object>) claims.get("video");
    }
}