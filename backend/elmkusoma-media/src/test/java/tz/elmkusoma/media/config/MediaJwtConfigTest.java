package tz.elmkusoma.media.config;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Configuration contract for the media JWT secret.
 *
 * <p>The shared HS256 key is Base64-decoded by {@code JwtAuthenticationFilter}
 * and {@code LocalMediaUrls}. An embedded default is never a valid option: the
 * previous placeholder was a plain sentence (illegal Base64 characters, shorter
 * than 256 bits), so a deployment without {@code JWT_SECRET} would only fail
 * at the first decode — or worse, sign with a weak key. The yml must therefore
 * reference the environment with no fallback, exactly like elmkusoma-core, so a
 * missing secret fails fast at startup.</p>
 */
class MediaJwtConfigTest {

    /** Same placeholder core uses — the two services share one secret. */
    private static final String EXPECTED_SECRET_PLACEHOLDER = "${JWT_SECRET}";

    @SuppressWarnings("unchecked")
    @Test
    void jwtSecret_isEnvironmentAuthoritative_withNoEmbeddedFallback() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/application.yml")) {
            assertNotNull(in, "media application.yml must be on the test classpath");

            Map<String, Object> yaml = new Yaml().load(in);
            Map<String, Object> jwt = (Map<String, Object>) yaml.get("jwt");
            assertNotNull(jwt, "the media yml must declare a jwt section");

            Object secret = jwt.get("secret");
            assertEquals(EXPECTED_SECRET_PLACEHOLDER, secret,
                    "jwt.secret must be the bare ${JWT_SECRET} placeholder — "
                            + "no insecure embedded default is allowed");
            assertTrue(String.valueOf(secret).indexOf(':') < 0,
                    "jwt.secret must not carry a ':default' suffix");
        }
    }
}
