package tz.elmkusoma.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link GlobalExceptionHandler} status mapping for the exceptions thrown by the
 * ownership / scope guards (plain SecurityException must surface as 403, not 500).
 */
class SecurityExceptionHandlingTest {

    @RestController
    static class ThrowingController {

        @GetMapping("/test/security-exception")
        void securityException() {
            throw new SecurityException("You do not own this lesson");
        }

        @GetMapping("/test/security-exception-null-message")
        void securityExceptionWithoutMessage() {
            throw new SecurityException();
        }

        @GetMapping("/test/resource-not-found")
        void resourceNotFound() {
            throw new ResourceNotFoundException("Lesson", "id", "abc");
        }

        @GetMapping("/test/forbidden")
        void forbidden() {
            throw new ForbiddenException("Forbidden");
        }

        @GetMapping("/test/access-denied")
        void accessDenied() {
            throw new AccessDeniedException("Access denied");
        }

        @GetMapping("/test/illegal-argument")
        void illegalArgument() {
            throw new IllegalArgumentException("Invalid lesson status: BOGUS");
        }
    }

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void securityExceptionIsMappedTo403() throws Exception {
        mockMvc.perform(get("/test/security-exception"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("You do not own this lesson"));
    }

    @Test
    void securityExceptionWithoutMessageIsMappedTo403WithFallbackBody() throws Exception {
        mockMvc.perform(get("/test/security-exception-null-message"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Access denied"));
    }

    @Test
    void resourceNotFoundExceptionIsMappedTo404() throws Exception {
        mockMvc.perform(get("/test/resource-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Lesson not found with id: 'abc'"));
    }

    @Test
    void forbiddenExceptionIsMappedTo403() throws Exception {
        mockMvc.perform(get("/test/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void springSecurityAccessDeniedIsMappedTo403() throws Exception {
        mockMvc.perform(get("/test/access-denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Access denied"));
    }

    @Test
    void illegalArgumentExceptionIsMappedTo400() throws Exception {
        mockMvc.perform(get("/test/illegal-argument"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid lesson status: BOGUS"));
    }
}
