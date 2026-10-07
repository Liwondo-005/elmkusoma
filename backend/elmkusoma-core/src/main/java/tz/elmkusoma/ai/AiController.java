package tz.elmkusoma.ai;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tz.elmkusoma.ai.dto.AiAskRequest;
import tz.elmkusoma.common.ApiResponse;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/v1/ai")
@RequiredArgsConstructor
@Tag(name = "AI", description = "AI assistant boundary - reports honestly when no provider is configured")
public class AiController {

    private final AiService aiService;

    @Value("${AI_PROVIDER:}")
    private String provider;

    @Value("${AI_API_KEY:}")
    private String apiKey;

    @PostMapping("/assistant/ask")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR')")
    @Operation(summary = "Ask the AI assistant a question")
    public ResponseEntity<ApiResponse<Map<String, String>>> ask(@Valid @RequestBody AiAskRequest request) {
        if (!isConfigured()) {
            return notConfigured();
        }
        String answer = aiService.ask(request.getQuestion(), null);
        return ResponseEntity.ok(ApiResponse.success(Map.of("answer", answer)));
    }

    @GetMapping("/health")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Report whether an AI provider is configured for this deployment")
    public ResponseEntity<Map<String, Object>> health() {
        boolean configured = isConfigured();
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("configured", configured);
        status.put("provider", configured ? provider : null);
        status.put("model", null);
        status.put("reason", configured
                ? "AI provider integration is not implemented in this deployment"
                : notConfiguredReason());
        return ResponseEntity.ok(status);
    }

    private boolean isConfigured() {
        return StringUtils.hasText(provider) && StringUtils.hasText(apiKey);
    }

    private String notConfiguredReason() {
        if (!StringUtils.hasText(provider)) {
            return "AI_PROVIDER not set";
        }
        return "AI_API_KEY not set";
    }

    private ResponseEntity<ApiResponse<Map<String, String>>> notConfigured() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error("AI is not configured in this deployment", "AI_NOT_CONFIGURED"));
    }
}
