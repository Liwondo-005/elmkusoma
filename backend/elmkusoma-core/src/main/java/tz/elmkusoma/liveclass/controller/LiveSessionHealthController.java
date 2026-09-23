package tz.elmkusoma.liveclass.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.liveclass.config.LiveKitConfig;
import tz.elmkusoma.liveclass.service.LiveKitService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/v1/live-session")
@RequiredArgsConstructor
@Tag(name = "Live Session Health", description = "Live service health and status")
public class LiveSessionHealthController {

    private final LiveKitConfig liveKitConfig;
    private final LiveKitService liveKitService;

    @GetMapping("/health")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Check live service health status")
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> health = new HashMap<>();
        boolean configured = liveKitConfig.isConfigured();
        health.put("service", "ELMKUSOMA Live");
        health.put("liveKitConfigured", configured);
        health.put("mode", configured ? "full" : "chat-only");

        if (configured) {
            health.put("status", "OPERATIONAL");
            health.put("message", "ELMKUSOMA Live is operational.");
        } else {
            health.put("status", "DEGRADED");
            health.put("message", "Live video service is not configured. Chat is available.");
        }

        return ResponseEntity.ok(health);
    }
}
