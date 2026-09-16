package tz.elmkusoma.workers.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/workers")
public class WorkerHealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "elmkusoma-workers");
        response.put("timestamp", LocalDateTime.now());
        response.put("workers", Map.of(
                "notificationConsumer", "active",
                "emailConsumer", "active",
                "certificateConsumer", "active",
                "schedulers", "active"
        ));

        return ResponseEntity.ok(response);
    }
}
