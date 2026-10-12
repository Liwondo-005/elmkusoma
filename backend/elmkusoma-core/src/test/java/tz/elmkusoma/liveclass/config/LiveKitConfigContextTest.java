package tz.elmkusoma.liveclass.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import tz.elmkusoma.liveclass.service.LiveKitService;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Boots the application with the recording flag that used to abort startup.
 *
 * <p>Before the fix, {@code livekit.egress-enabled=true} produced:</p>
 * <pre>APPLICATION FAILED TO START ... No setter found for property: egress-enabled</pre>
 * so the flag could never be switched on and recording was impossible to enable.
 *
 * <p>API credentials are supplied here so {@code isEgressEnabled()} is genuinely true: the
 * storage destination is intentionally left unset, because this test is about the flag binding
 * and context startup, not about a real recording destination.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        // application-test.properties pins livekit.egress.enabled=false so the suite never
        // touches a real recorder. Both spellings are set here so they cannot disagree:
        // the flat form is the regression under test, the nested one keeps the two writes
        // to the same field from racing.
        "livekit.egress-enabled=true",
        "livekit.egress.enabled=true",
        "livekit.server.url=ws://localhost:7880",
        "livekit.server.api-key=test-key",
        "livekit.server.api-secret=test-secret"
})
class LiveKitConfigContextTest {

    @Autowired
    private LiveKitConfig liveKitConfig;

    @Autowired
    private LiveKitService liveKitService;

    @Test
    void applicationStartsWithEgressEnabled() {
        assertNotNull(liveKitConfig, "the application context must start");
        assertTrue(liveKitConfig.getEgress().isEnabled(),
                "livekit.egress-enabled must switch the recording flag on");
        assertTrue(liveKitConfig.isEgressEnabled(),
                "with API credentials present, egress reports enabled");
    }

    /** Enabling the flag must not fake a usable recorder: storage is still required. */
    @Test
    void enabledEgress_withoutStorageIsStillNotRecordable() {
        assertTrue(!liveKitConfig.getEgress().hasStorage(),
                "no bucket/credentials were configured for this test");
        assertTrue(!liveKitService.isRecordingConfigured(),
                "a recording destination is required before recording may be started");
    }
}