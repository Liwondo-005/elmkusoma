package tz.elmkusoma.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tz.elmkusoma.config.security.SendCodeRateLimitFilter;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Global send-code rate limiting: a fixed per-IP window on
 * {@code POST /v1/auth/send-code} (the per-email 60s cooldown is enforced
 * separately in {@code AuthServiceImpl}).
 *
 * <p>Runs in its own Spring context ({@code send-code-per-minute=3}) so the
 * shared in-memory window cannot leak into or from other suites. One test
 * method by design — the window is per-context and order-sensitive.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(
        locations = "classpath:application-test.properties",
        properties = "elmkusoma.security.send-code-per-minute=3")
class SendCodeRateLimitFilterTest {

    @Autowired
    private MockMvc mockMvc;

    private static String freshEmail() {
        return "ratelimit-" + UUID.randomUUID() + "@test.com";
    }

    @Test
    void burstWithinWindow_isCutOffAt429_withRetryAfter() throws Exception {
        // First three distinct-email sends pass the cooldown and the limit.
        for (int i = 1; i <= 3; i++) {
            mockMvc.perform(MockMvcRequestBuilders.post("/v1/auth/send-code")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + freshEmail() + "\"}"))
                    .andExpect(status().isOk());
        }

        // Fourth send in the same window: 429 with the standard error shape.
        MvcResult fourth = mockMvc.perform(MockMvcRequestBuilders.post("/v1/auth/send-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + freshEmail() + "\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value(SendCodeRateLimitFilter.RATE_LIMIT_MESSAGE))
                .andReturn();
        assertEquals("application/json",
                fourth.getResponse().getContentType().split(";")[0].trim());

        // Still limited on the next attempt (same window).
        mockMvc.perform(MockMvcRequestBuilders.post("/v1/auth/send-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + freshEmail() + "\"}"))
                .andExpect(status().isTooManyRequests());
    }
}
