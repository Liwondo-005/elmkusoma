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
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Regression test for the {@code FRONTEND_URL} wiring defect.
 *
 * <p>{@code backend/.env.example} advertised {@code FRONTEND_URL}, but nothing in
 * {@code application.yml} mapped that name onto the {@code app.frontend-url} property that
 * {@code AuthServiceImpl} and {@code PlatformAdminService} actually read. Setting it therefore
 * had no effect and every emailed recovery link silently pointed at
 * {@code http://localhost:3000}.</p>
 *
 * <p>This test loads the repository's real {@code application.yml} into a Spring
 * {@link StandardEnvironment} - so the system-properties and system-environment sources that
 * the {@code .env} loader writes to are present with their production precedence - and then
 * resolves {@code app.frontend-url} exactly as {@code @Value} would.</p>
 */
class FrontendUrlBindingTest {

    private static final String SENTINEL = "https://recovery-links-are-configured.test";

    private String previousFrontendUrl;
    private String previousAppFrontendUrl;

    @BeforeEach
    void isolateTheRealEnvironment() {
        previousFrontendUrl = System.getProperty("FRONTEND_URL");
        previousAppFrontendUrl = System.getProperty("APP_FRONTEND_URL");
        clear("FRONTEND_URL");
        clear("APP_FRONTEND_URL");
    }

    @AfterEach
    void restoreTheRealEnvironment() {
        restore("FRONTEND_URL", previousFrontendUrl);
        restore("APP_FRONTEND_URL", previousAppFrontendUrl);
    }

    @Test
    void frontendUrlSystemProperty_reachesAppFrontendUrl() throws IOException {
        System.setProperty("FRONTEND_URL", SENTINEL);

        assertEquals(SENTINEL, environment().getProperty("app.frontend-url"),
                "FRONTEND_URL (the name backend/.env documents) must drive app.frontend-url");
    }

    @Test
    void appFrontendUrlSystemProperty_reachesAppFrontendUrl() throws IOException {
        System.setProperty("APP_FRONTEND_URL", SENTINEL);

        assertEquals(SENTINEL, environment().getProperty("app.frontend-url"),
                "Spring's relaxed-binding form must keep working too");
    }

    @Test
    void frontendUrlWinsOverAppFrontendUrl() throws IOException {
        System.setProperty("APP_FRONTEND_URL", "https://secondary.test");
        System.setProperty("FRONTEND_URL", SENTINEL);

        assertEquals(SENTINEL, environment().getProperty("app.frontend-url"),
                "the documented .env name takes precedence");
    }

    @Test
    void withNothingSet_theDocumentedLocalDefaultApplies() throws IOException {
        assertEquals("http://localhost:3000", environment().getProperty("app.frontend-url"),
                "local development must keep its default so recovery tests stay deterministic");
    }

    /** The property must exist in the file, not merely fall back to the @Value default. */
    @Test
    void applicationYmlDeclaresThePropertyExplicitly() throws IOException {
        // getProperty(String) with no default returns null when nothing declares the key.
        // The @Value default on the consumers would still supply a value inside the app, so
        // resolving a non-null answer here proves application.yml itself maps it.
        assertNotNull(environment().getProperty("app.frontend-url"),
                "application.yml must declare app.frontend-url, not leave it to @Value defaults");
    }

    // ------------------------------------------------------------------ helpers

    private static StandardEnvironment environment() throws IOException {
        StandardEnvironment env = new StandardEnvironment();
        List<PropertySource<?>> sources =
                new YamlPropertySourceLoader().load("application", new ClassPathResource("application.yml"));
        for (PropertySource<?> source : sources) {
            env.getPropertySources().addLast(source);
        }
        return env;
    }

    private static void clear(String key) {
        String value = System.getProperty(key);
        if (value != null) {
            System.clearProperty(key);
        }
    }

    private static void restore(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
}
