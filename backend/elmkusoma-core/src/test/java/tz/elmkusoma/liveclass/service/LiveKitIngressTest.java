package tz.elmkusoma.liveclass.service;

import org.junit.jupiter.api.Test;
import tz.elmkusoma.liveclass.config.LiveKitConfig;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards for the external-ingest (OBS/encoder/studio) integration:
 * - disabled by default → honest "not available", never a fabricated endpoint;
 * - enabled but server unreachable → real failure surfaces as null (no fake
 *   URL/stream key is invented);
 * - unsupported protocol → explicit validation error.
 */
class LiveKitIngressTest {

    private LiveKitService service(LiveKitConfig config) {
        return new LiveKitService(config);
    }

    /** Production always configures credentials (application.yml defaults devkey/…); mirror that. */
    private LiveKitConfig configuredServer() {
        LiveKitConfig config = new LiveKitConfig();
        config.getServer().setApiKey("devkey");
        config.getServer().setApiSecret("devsecret-devsecret-devsecret-dev01!");
        return config;
    }

    @Test
    void ingress_disabledByDefault() {
        LiveKitService svc = service(configuredServer());
        assertFalse(svc.isIngressConfigured());
        assertTrue(svc.listIngress(UUID.randomUUID()).isEmpty());
    }

    @Test
    void createIngress_whenDisabled_returnsNullNotAFakeEndpoint() {
        LiveKitService svc = service(configuredServer());
        assertNull(svc.createIngress(UUID.randomUUID(), "WHIP", "identity", "name"));
        assertFalse(svc.deleteIngress("ingress-123"));
    }

    @Test
    void createIngress_enabledButServerUnreachable_returnsNullWithoutFabricatedData() {
        LiveKitConfig config = configuredServer();
        config.getIngress().setEnabled(true);
        // Nothing listens on this port → connection refused. The service must
        // report the real failure instead of inventing inputUrl/streamKey.
        config.getServer().setUrl("ws://127.0.0.1:1");

        LiveKitService svc = service(config);
        assertTrue(svc.isIngressConfigured());

        Map<String, String> result = svc.createIngress(UUID.randomUUID(), "RTMP", "identity", "name");
        assertNull(result);
    }

    @Test
    void createIngress_unsupportedProtocol_throwsValidationError() {
        LiveKitConfig config = configuredServer();
        config.getIngress().setEnabled(true);
        LiveKitService svc = service(config);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> svc.createIngress(UUID.randomUUID(), "FTP", "identity", "name"));
        assertTrue(ex.getMessage().contains("WHIP"));
    }

    @Test
    void createIngress_protocolMapping_coversWhipRtmpSrt() {
        LiveKitConfig config = configuredServer();
        config.getIngress().setEnabled(true);
        // Dead port: validation runs before the network path, so accepted
        // protocols fall through to a real (failed) call instead of touching a
        // locally-running LiveKit server, while unsupported ones must throw.
        config.getServer().setUrl("ws://127.0.0.1:1");
        LiveKitService svc = service(config);
        for (String protocol : new String[]{"WHIP", "RTMP", "SRT", "whip", "rtmp", "srt"}) {
            try {
                svc.createIngress(UUID.randomUUID(), protocol, "identity", "name");
            } catch (IllegalArgumentException unexpected) {
                throw new AssertionError("Protocol should be accepted: " + protocol, unexpected);
            }
        }
    }
}
