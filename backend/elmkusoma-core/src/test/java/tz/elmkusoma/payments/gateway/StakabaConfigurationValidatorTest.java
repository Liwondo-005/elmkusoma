package tz.elmkusoma.payments.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Startup configuration rules for the gateway. A disabled provider (the
 * default) must never block startup; an enabled one must fail fast with a
 * message naming the environment variable - never the credential - when the
 * configuration cannot safely run, most importantly when a LIVE key appears
 * in what is supposed to be a sandbox deployment.
 */
class StakabaConfigurationValidatorTest {

    private static StakabaProperties props(boolean enabled, String baseUrl, String apiKey,
                                           boolean allowLive) {
        StakabaProperties p = new StakabaProperties();
        p.setEnabled(enabled);
        p.setBaseUrl(baseUrl);
        p.setApiKey(apiKey);
        p.setAllowLive(allowLive);
        return p;
    }

    @Test
    void disabledProviderNeedsNoCredentials() {
        assertDoesNotThrow(() -> new StakabaConfigurationValidator(
                props(false, "https://api.stakaba.com", "", false)).validate());
    }

    @Test
    void enabledWithoutKeyFailsFastNamingTheEnvVar() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new StakabaConfigurationValidator(
                        props(true, "https://api.stakaba.com", "", false)).validate());
        assertTrue(ex.getMessage().contains("STAKABA_API_KEY"),
                "message must name the variable to set: " + ex.getMessage());
        // The message may mention the documented key PREFIX as guidance
        // (sk_test_...) but must never echo credential material - i.e. a key
        // prefix followed by real key characters.
        assertTrue(!ex.getMessage().matches("(?s).*sk_(test|live)_[A-Za-z0-9]{6,}.*"),
                "message must never contain key material: " + ex.getMessage());
    }

    @Test
    void enabledWithSandboxKeyStarts() {
        assertDoesNotThrow(() -> new StakabaConfigurationValidator(
                props(true, "https://api.stakaba.com", "sk_test_abc123", false)).validate());
    }

    @Test
    void liveKeyIsRefusedWithoutTheExplicitInterlock() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new StakabaConfigurationValidator(
                        props(true, "https://api.stakaba.com", "sk_live_abc123", false)).validate());
        assertTrue(ex.getMessage().contains("sk_live_"),
                "message should explain the live-key refusal");
        assertTrue(ex.getMessage().contains("sk_test_"),
                "message should point at the sandbox key");
    }

    @Test
    void liveKeyWithInterlockAllowsStartupButNeverActivatesItByDefault() {
        assertDoesNotThrow(() -> new StakabaConfigurationValidator(
                props(true, "https://api.stakaba.com", "sk_live_abc123", true)).validate());
        // The interlock defaults to false in StakabaProperties itself.
        assertTrue(!new StakabaProperties().isAllowLive(),
                "allow-live must default to false");
    }

    @Test
    void nonHttpsBaseUrlIsRefused() {
        assertThrows(IllegalStateException.class,
                () -> new StakabaConfigurationValidator(
                        props(true, "http://api.stakaba.com", "sk_test_abc", false)).validate());
    }

    @Test
    void keyThatIsNotASkabaKeyIsRefused() {
        assertThrows(IllegalStateException.class,
                () -> new StakabaConfigurationValidator(
                        props(true, "https://api.stakaba.com", "random-string", false)).validate());
    }
}
