package tz.elmkusoma.liveclass.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Configuration validation for LiveKit Egress + Backblaze B2.
 *
 * <p>Two guarantees are pinned here:</p>
 * <ol>
 *   <li>A half-configured recorder is diagnosed precisely, by setting <em>name</em>.</li>
 *   <li>It never aborts startup. Recording storage is optional, so an incomplete value must
 *       not stop the rest of the platform booting - it is the recording feature alone that
 *       stays switched off.</li>
 * </ol>
 */
class LiveKitEgressConfigValidatorTest {

    private static final String SENTINEL_ACCESS_KEY = "AKIA-SENTINEL-ACCESS-KEY-9F3";
    private static final String SENTINEL_SECRET = "SENTINEL-SECRET-VALUE-c71a2b";

    // --------------------------------------------------------------- switched off

    @Test
    void recordingDisabled_neverReportsAnything() {
        LiveKitConfig config = new LiveKitConfig(); // enabled defaults to false
        assertTrue(LiveKitEgressConfigValidator.configurationProblems(config).isEmpty(),
                "with recording off there is nothing to validate");
        assertDoesNotThrow(() -> new LiveKitEgressConfigValidator(config).afterPropertiesSet());
    }

    @Test
    void recordingDisabled_withNoStorageAtAll_stillStarts() {
        LiveKitConfig config = new LiveKitConfig();
        config.getEgress().setEnabled(false);
        assertDoesNotThrow(() -> new LiveKitEgressConfigValidator(config).afterPropertiesSet(),
                "a dev/test run without any recording credentials must boot normally");
    }

    // ---------------------------------------------------------------- half-set up

    @Test
    void enabledWithNothing_configuresListEveryMissingSetting() {
        LiveKitConfig config = new LiveKitConfig();
        config.getEgress().setEnabled(true);

        List<String> problems = LiveKitEgressConfigValidator.configurationProblems(config);
        String all = String.join(" | ", problems);

        assertTrue(all.contains("LIVEKIT_API_KEY"), all);
        assertTrue(all.contains("LIVEKIT_EGRESS_BUCKET"), all);
        assertTrue(all.contains("LIVEKIT_EGRESS_S3_ACCESS_KEY"), all);
        assertTrue(all.contains("LIVEKIT_EGRESS_S3_SECRET"), all);
        assertTrue(all.contains("LIVEKIT_EGRESS_S3_REGION"), all);
    }

    @Test
    void enabledWithStorageButNoApiCredentials_stillReportsTheApiCredentials() {
        LiveKitConfig config = fullyConfigured();
        config.getServer().setApiKey("");
        config.getServer().setApiSecret("");

        List<String> problems = LiveKitEgressConfigValidator.configurationProblems(config);
        assertTrue(String.join(" ", problems).contains("LIVEKIT_API_KEY"),
                "an egress job cannot be created without API credentials");
    }

    @Test
    void enabledWithBucketButNoRegionOrEndpoint_isRejected() {
        LiveKitConfig config = fullyConfigured();
        config.getEgress().setRegion("");
        config.getEgress().setEndpoint("");

        assertTrue(String.join(" ",
                        LiveKitEgressConfigValidator.configurationProblems(config))
                .contains("LIVEKIT_EGRESS_S3_REGION"));
    }

    @Test
    void malformedEndpoint_isRejected() {
        LiveKitConfig config = fullyConfigured();
        config.getEgress().setEndpoint("s3.us-east-005.backblazeb2.com"); // no scheme

        assertTrue(String.join(" ",
                        LiveKitEgressConfigValidator.configurationProblems(config))
                .contains("LIVEKIT_EGRESS_S3_ENDPOINT"), "a scheme-less endpoint would not work");
    }

    @Test
    void emptyEndpointWithRegion_isAccepted() {
        LiveKitConfig config = fullyConfigured();
        config.getEgress().setEndpoint("");
        assertTrue(LiveKitEgressConfigValidator.configurationProblems(config).isEmpty(),
                "AWS S3 needs no custom endpoint, so region alone is sufficient");
    }

    // ----------------------------------------------------------------- complete

    @Test
    void fullyConfiguredRecorder_reportsNothing() {
        assertTrue(LiveKitEgressConfigValidator.configurationProblems(fullyConfigured()).isEmpty());
    }

    @Test
    void backblazeB2StyleConfiguration_isAccepted() {
        LiveKitConfig config = new LiveKitConfig();
        config.getServer().setApiKey("key");
        config.getServer().setApiSecret("secret");
        config.getEgress().setEnabled(true);
        config.getEgress().setOutputBucket("elmkusoma-live-recordings");
        config.getEgress().setAccessKey(SENTINEL_ACCESS_KEY);
        config.getEgress().setSecret(SENTINEL_SECRET);
        config.getEgress().setRegion("us-east-005");
        config.getEgress().setEndpoint("https://s3.us-east-005.backblazeb2.com");

        assertTrue(LiveKitEgressConfigValidator.configurationProblems(config).isEmpty());
        assertDoesNotThrow(() -> new LiveKitEgressConfigValidator(config).afterPropertiesSet());
    }

    // ---------------------------------------------------------------- security

    /** The whole point of "redacted": names yes, values never. */
    @Test
    void diagnostics_neverEchoCredentialValues() {
        LiveKitConfig config = new LiveKitConfig();
        config.getEgress().setEnabled(true);
        config.getEgress().setAccessKey(SENTINEL_ACCESS_KEY);
        config.getEgress().setSecret(SENTINEL_SECRET);   // bucket/region still missing
        config.getEgress().setEndpoint("not-a-url");

        String reported = String.join(" ; ",
                LiveKitEgressConfigValidator.configurationProblems(config));

        assertFalse(reported.contains(SENTINEL_ACCESS_KEY), "access key leaked: " + reported);
        assertFalse(reported.contains(SENTINEL_SECRET), "secret leaked: " + reported);
        assertTrue(reported.contains("LIVEKIT_EGRESS_BUCKET"),
                "the missing setting must still be named: " + reported);
    }

    /** An incomplete recorder is a recording problem, never a boot problem. */
    @Test
    void incompleteConfiguration_doesNotAbortStartup() {
        LiveKitConfig config = new LiveKitConfig();
        config.getEgress().setEnabled(true);
        assertFalse(LiveKitEgressConfigValidator.configurationProblems(config).isEmpty(),
                "sanity: this configuration is incomplete");

        assertDoesNotThrow(() -> new LiveKitEgressConfigValidator(config).afterPropertiesSet(),
                "an optional subsystem must not stop the platform booting");
    }

    // ------------------------------------------------------------------ helpers

    private static LiveKitConfig fullyConfigured() {
        LiveKitConfig config = new LiveKitConfig();
        config.getServer().setApiKey("key");
        config.getServer().setApiSecret("secret");
        config.getEgress().setEnabled(true);
        config.getEgress().setOutputBucket("elmkusoma-live-recordings");
        config.getEgress().setAccessKey(SENTINEL_ACCESS_KEY);
        config.getEgress().setSecret(SENTINEL_SECRET);
        config.getEgress().setRegion("us-east-005");
        config.getEgress().setEndpoint("https://s3.us-east-005.backblazeb2.com");
        return config;
    }
}
