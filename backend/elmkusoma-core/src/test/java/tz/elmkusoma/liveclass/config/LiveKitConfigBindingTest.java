package tz.elmkusoma.liveclass.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for the recording-enablement failure.
 *
 * <p>{@code LIVEKIT_EGRESS_ENABLED} used to make the whole application fail to boot:</p>
 *
 * <pre>
 * Failed to bind properties under 'livekit' to LiveKitConfig
 *   Property: livekit.egress-enabled
 *   Reason: java.lang.IllegalStateException: No setter found for property: egress-enabled
 * </pre>
 *
 * <p>Spring relaxed-binds that variable to the top-level {@code egressEnabled} property, which
 * only had the computed getter {@link LiveKitConfig#isEgressEnabled()}. Because binding aborted
 * startup, the flag had to remain unset, so recording could never be switched on at all.</p>
 *
 * <p>These tests drive the real Spring binder rather than a mock, so they fail for the same
 * reason the application did.</p>
 */
class LiveKitConfigBindingTest {

    private LiveKitConfig bind(Map<String, String> properties) {
        return new Binder(new MapConfigurationPropertySource(properties))
                .bind("livekit", LiveKitConfig.class)
                .get();
    }

    /** The exact environment variable that used to crash startup must now bind. */
    @Test
    void egressEnabled_bindsThroughTheEnvironmentVariableForm() {
        LiveKitConfig config = bind(Map.of("livekit.egress-enabled", "true"));
        assertTrue(config.getEgress().isEnabled(),
                "livekit.egress-enabled (LIVEKIT_EGRESS_ENABLED) must switch recording on");
    }

    /** The nested form documented for the other LiveKit settings keeps working. */
    @Test
    void egressEnabled_bindsThroughTheNestedForm() {
        LiveKitConfig config = bind(Map.of("livekit.egress.enabled", "true"));
        assertTrue(config.getEgress().isEnabled());
    }

    @Test
    void egressEnabled_bindsFalse() {
        LiveKitConfig config = bind(Map.of("livekit.egress-enabled", "false"));
        assertFalse(config.getEgress().isEnabled());
    }

    @Test
    void ingressEnabled_bindsWithoutFailing() {
        LiveKitConfig config = bind(Map.of("livekit.ingress-enabled", "true"));
        assertTrue(config.getIngress().isEnabled());
    }

    /**
     * A value bound to one of the computed, setter-less helpers is rejected by the raw binder.
     * That is expected: ignoreInvalidFields is applied by ConfigurationPropertiesBinder when
     * the bean is created by Spring, which is what keeps the application context alive. The
     * context-level guarantee is asserted in LiveKitConfigContextTest, which boots the app.
     */
    @Test
    void valueOnAComputedProperty_isRejectedByTheRawBinder_butNotFatalInTheApp() {
        assertThrows(org.springframework.boot.context.properties.bind.BindException.class,
                () -> bind(Map.of("livekit.configured", "true")),
                "the bare binder has no ignore-invalid handler; the application does");
        // The supported key is unaffected by that.
        assertTrue(bind(Map.of("livekit.egress-enabled", "true")).getEgress().isEnabled());
    }

    /** Enablement alone is not enough: recording still needs an S3 destination to exist. */
    @Test
    void egressEnabled_aloneStillReportsStorageMissing() {
        LiveKitConfig config = bind(Map.of(
                "livekit.server.api-key", "key",
                "livekit.server.api-secret", "secret",
                "livekit.egress-enabled", "true"));

        assertTrue(config.isEgressEnabled(), "with credentials present, egress is enabled");
        assertFalse(config.getEgress().hasStorage(),
                "without a bucket/key/secret there is nowhere to write the recording");
    }

    @Test
    void egressEnabled_withStorage_isFullyConfigured() {
        LiveKitConfig config = bind(Map.of(
                "livekit.server.api-key", "key",
                "livekit.server.api-secret", "secret",
                "livekit.egress-enabled", "true",
                "livekit.egress.output-bucket", "bucket",
                "livekit.egress.access-key", "ak",
                "livekit.egress.secret", "sk",
                "livekit.egress.region", "us-east-1"));

        assertTrue(config.isEgressEnabled());
        assertTrue(config.getEgress().hasStorage());
    }

    /** Credentials are still required: the flag alone must not imply a usable recorder. */
    @Test
    void egressEnabled_withoutApiCredentials_isNotEnabled() {
        LiveKitConfig config = bind(Map.of("livekit.egress-enabled", "true"));
        assertFalse(config.isEgressEnabled(),
                "isEgressEnabled must still require API key and secret");
    }
}