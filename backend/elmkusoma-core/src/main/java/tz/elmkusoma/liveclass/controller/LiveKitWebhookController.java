package tz.elmkusoma.liveclass.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/webhooks/livekit")
@RequiredArgsConstructor
@Slf4j
public class LiveKitWebhookController {

    @PostMapping
    public ResponseEntity<Map<String, String>> handleWebhook(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "Authorization", required = false) String auth) {

        String event = (String) payload.get("event");
        log.info("LiveKit webhook received: event={}", event);

        if (event == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing event field"));
        }

        switch (event) {
            case "room_started" -> log.info("Room started: {}", payload.get("room"));
            case "room_ended" -> log.info("Room ended: {}", payload.get("room"));
            case "participant_joined" -> log.info("Participant joined: {}", payload.get("participant"));
            case "participant_left" -> log.info("Participant left: {}", payload.get("participant"));
            case "recording_started" -> log.info("Recording started: {}", payload.get("egress"));
            case "recording_completed" -> {
                log.info("Recording completed: {}", payload.get("egress"));
            }
            case "recording_failed" -> {
                log.error("Recording failed: {}", payload.get("egress"));
            }
            default -> log.debug("Unhandled webhook event: {}", event);
        }

        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
