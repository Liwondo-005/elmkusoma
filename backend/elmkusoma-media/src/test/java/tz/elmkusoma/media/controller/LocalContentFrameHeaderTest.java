package tz.elmkusoma.media.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import tz.elmkusoma.media.config.SecurityConfig;
import tz.elmkusoma.media.service.impl.LocalStorageService;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The core learner viewer embeds local-content in a cross-origin iframe
 * (app on :3000, media on :8083). Spring Security's default
 * {@code X-Frame-Options: DENY} blanked that viewer, so the header is lifted
 * for this endpoint only — the rest of the service keeps the default.
 *
 * <p>Standalone configuration (instead of the {@code @SpringBootApplication}
 * entry point) so the slice does not drag in {@code @EnableJpaRepositories},
 * Redis, or a datasource — none of which this controller touches.</p>
 */
@SpringBootTest(
        classes = LocalContentFrameHeaderTest.TestConfig.class,
        properties = {
                "media.storage=local",
                "media.local.dir=${java.io.tmpdir}/elmkusoma-frame-test",
                // The filter Base64-decodes the shared HS256 secret; the yml
                // fallback (a plain sentence) is not Base64, so the slice needs
                // a real one — deployments inject JWT_SECRET from the environment.
                "jwt.secret=dGVzdC1qd3Qtc2VjcmV0LWtleS10aGF0LWlzLWJhc2U2NA=="
        })
@AutoConfigureMockMvc
class LocalContentFrameHeaderTest {

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            JpaRepositoriesAutoConfiguration.class,
            RedisAutoConfiguration.class
    })
    @Import({SecurityConfig.class, LocalContentController.class, LocalStorageService.class})
    static class TestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void storeAnObject() throws Exception {
        Path file = Path.of(System.getProperty("java.io.tmpdir"),
                "elmkusoma-frame-test", "inst", "media", "file.pdf");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "%PDF-1.4 test");
    }

    @Test
    void localContent_successResponse_mayBeFramedByTheCoreViewer() throws Exception {
        mockMvc.perform(get("/api/v1/media/local-content")
                        .param("key", "inst/media/file.pdf"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("X-Frame-Options"));
    }

    @Test
    void localContent_invalidSignature_deniedWithoutFrameHeader() throws Exception {
        mockMvc.perform(get("/api/v1/media/local-content")
                        .param("key", "inst/media/file.pdf")
                        .param("exp", "1")
                        .param("sig", "bad-signature"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("X-Frame-Options"));
    }

    @Test
    void otherEndpoints_keepFrameDenial() throws Exception {
        mockMvc.perform(get("/api/v1/media/not-a-real-endpoint"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }
}
