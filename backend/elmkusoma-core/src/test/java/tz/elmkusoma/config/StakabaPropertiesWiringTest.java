package tz.elmkusoma.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins the explicit {@code application.yml} mapping of the {@code STAKABA_*}
 * environment variables (the names {@code backend/.env} supplies) onto the
 * {@code payment.stakaba.*} properties the gateway reads, plus the fail-safe
 * defaults: gateway disabled, sandbox base URL, no key, live interlock off.
 */
class StakabaPropertiesWiringTest {

    private static final String[] KEYS = {
            "STAKABA_ENABLED", "STAKABA_BASE_URL", "STAKABA_API_KEY",
            "STAKABA_CONNECT_TIMEOUT_MS", "STAKABA_READ_TIMEOUT_MS", "STAKABA_ALLOW_LIVE",
            "STAKABA_SEND_BEARER_FALLBACK"
    };
    private final String[] previous = new String[KEYS.length];

    @BeforeEach
    void isolateTheRealEnvironment() {
        for (int i = 0; i < KEYS.length; i++) {
            previous[i] = System.getProperty(KEYS[i]);
            System.clearProperty(KEYS[i]);
        }
    }

    @AfterEach
    void restoreTheRealEnvironment() {
        for (int i = 0; i < KEYS.length; i++) {
            if (previous[i] == null) {
                System.clearProperty(KEYS[i]);
            } else {
                System.setProperty(KEYS[i], previous[i]);
            }
        }
    }

    @Test
    void stakabaEnvVarsReachThePropertiesTheGatewayReads() throws IOException {
        System.setProperty("STAKABA_ENABLED", "true");
        System.setProperty("STAKABA_BASE_URL", "https://api.stakaba.com");
        System.setProperty("STAKABA_API_KEY", "sk_test_wiring-sentinel");
        System.setProperty("STAKABA_CONNECT_TIMEOUT_MS", "1234");
        System.setProperty("STAKABA_READ_TIMEOUT_MS", "4321");
        System.setProperty("STAKABA_ALLOW_LIVE", "false");

        StandardEnvironment env = environment();
        assertEquals("true", env.getProperty("payment.stakaba.enabled"));
        assertEquals("https://api.stakaba.com", env.getProperty("payment.stakaba.base-url"));
        assertEquals("sk_test_wiring-sentinel", env.getProperty("payment.stakaba.api-key"));
        assertEquals("1234", env.getProperty("payment.stakaba.connect-timeout-ms"));
        assertEquals("4321", env.getProperty("payment.stakaba.read-timeout-ms"));
        assertEquals("false", env.getProperty("payment.stakaba.allow-live"));
    }

    @Test
    void defaultsAreSandboxDisabledAndFailSafe() throws IOException {
        StandardEnvironment env = environment();
        assertEquals("false", env.getProperty("payment.stakaba.enabled"),
                "the gateway must be OFF unless explicitly enabled");
        assertEquals("https://api.stakaba.com", env.getProperty("payment.stakaba.base-url"),
                "default base URL must be the documented sandbox/live URL");
        assertEquals("", env.getProperty("payment.stakaba.api-key"),
                "no key may be baked into the config");
        assertEquals("false", env.getProperty("payment.stakaba.allow-live"),
                "the live-key interlock must default to false");
        assertEquals("true", env.getProperty("payment.stakaba.send-bearer-fallback"),
                "the documented dual-auth hedge defaults on for the status lookup");
        assertEquals("none", env.getProperty("payment.provider"),
                "no outbound provider is selected by default");
    }

    private static StandardEnvironment environment() throws IOException {
        StandardEnvironment env = new StandardEnvironment();
        List<PropertySource<?>> sources =
                new YamlPropertySourceLoader().load("application", new ClassPathResource("application.yml"));
        for (PropertySource<?> source : sources) {
            env.getPropertySources().addLast(source);
        }
        return env;
    }
}
