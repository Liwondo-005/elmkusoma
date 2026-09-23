package tz.elmkusoma.liveclass.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.event.domain.Event;
import tz.elmkusoma.event.domain.Replay;
import tz.elmkusoma.event.repository.EventRepository;
import tz.elmkusoma.event.repository.ReplayRepository;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/webhooks/livekit")
@RequiredArgsConstructor
@Slf4j
public class LiveKitWebhookController {

    private final EventRepository eventRepository;
    private final ReplayRepository replayRepository;
    private final tz.elmkusoma.administration.service.PlatformIntegrationService integrationService;

    @PostMapping
    public ResponseEntity<Map<String, String>> handleWebhook(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "Authorization", required = false) String auth) {

        String event = (String) payload.get("event");
        log.info("LiveKit webhook received: event={}", event);

        if (event == null) {
            integrationService.recordWebhook("LIVEKIT", "malformed", "FAILED", "FAILED", "Missing event field");
            return ResponseEntity.badRequest().body(Map.of("error", "Missing event field"));
        }

        try {
            switch (event) {
                case "room_started" -> log.info("Room started: {}", payload.get("room"));
                case "room_ended" -> log.info("Room ended: {}", payload.get("room"));
                case "participant_joined" -> log.info("Participant joined: {}", payload.get("participant"));
                case "participant_left" -> log.info("Participant left: {}", payload.get("participant"));
                case "recording_started" -> {
                    log.info("Recording started: {}", payload.get("egress"));
                    handleRecordingStarted(payload);
                }
                case "recording_completed" -> {
                    log.info("Recording completed: {}", payload.get("egress"));
                    handleRecordingCompleted(payload);
                }
                case "recording_failed" -> {
                    log.error("Recording failed: {}", payload.get("egress"));
                    handleRecordingFailed(payload);
                }
                default -> log.debug("Unhandled webhook event: {}", event);
            }
            integrationService.recordWebhook("LIVEKIT", event, "UNVERIFIED", "SUCCESS", null);
        } catch (Exception e) {
            integrationService.recordWebhook("LIVEKIT", event, "UNVERIFIED", "FAILED", e.getMessage());
            throw e;
        }

        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    private void handleRecordingStarted(Map<String, Object> payload) {
        try {
            Map<String, Object> room = (Map<String, Object>) payload.get("room");
            if (room == null) return;
            String roomName = (String) room.get("name");
            if (roomName == null) return;

            UUID eventId = extractEventIdFromRoomName(roomName);
            if (eventId == null) return;

            eventRepository.findById(eventId).ifPresent(event -> {
                event.setRecordingStatus("PROCESSING");
                eventRepository.save(event);
                log.info("Event recording status set to PROCESSING: eventId={}", eventId);
            });
        } catch (Exception e) {
            log.error("Error handling recording_started webhook", e);
        }
    }

    private void handleRecordingCompleted(Map<String, Object> payload) {
        try {
            Map<String, Object> egress = (Map<String, Object>) payload.get("egress");
            Map<String, Object> room = (Map<String, Object>) payload.get("room");
            if (room == null) return;
            String roomName = (String) room.get("name");
            if (roomName == null) return;

            UUID eventId = extractEventIdFromRoomName(roomName);
            if (eventId == null) return;

            String recordingUrl = null;
            if (egress != null) {
                Object fileResults = egress.get("fileResults");
                if (fileResults instanceof java.util.List<?> files && !files.isEmpty()) {
                    Map<String, Object> firstFile = (Map<String, Object>) files.get(0);
                    recordingUrl = (String) firstFile.get("url");
                }
            }
            final String finalRecordingUrl = recordingUrl;

            eventRepository.findById(eventId).ifPresent(event -> {
                event.setRecordingStatus("AVAILABLE");
                if (finalRecordingUrl != null) {
                    event.setRecordingUrl(finalRecordingUrl);
                }
                eventRepository.save(event);

                boolean hasReplay = replayRepository.findByEventIdAndIsDeletedFalse(eventId).stream()
                        .anyMatch(r -> "AVAILABLE".equals(r.getStatus()));
                if (!hasReplay) {
                    Replay replay = Replay.builder()
                            .eventId(eventId)
                            .title(event.getTitle())
                            .description("Replay of " + event.getTitle())
                            .recordingUrl(finalRecordingUrl)
                            .status("AVAILABLE")
                            .viewCount(0)
                            .lastPositionSeconds(0)
                            .institutionId(event.getInstitutionId())
                            .build();
                    replayRepository.save(replay);
                    log.info("Replay created from webhook: eventId={}, replayId={}", eventId, replay.getId());
                }
            });
        } catch (Exception e) {
            log.error("Error handling recording_completed webhook", e);
        }
    }

    private void handleRecordingFailed(Map<String, Object> payload) {
        try {
            Map<String, Object> room = (Map<String, Object>) payload.get("room");
            if (room == null) return;
            String roomName = (String) room.get("name");
            if (roomName == null) return;

            UUID eventId = extractEventIdFromRoomName(roomName);
            if (eventId == null) return;

            eventRepository.findById(eventId).ifPresent(event -> {
                event.setRecordingStatus("FAILED");
                eventRepository.save(event);
                log.info("Event recording status set to FAILED: eventId={}", eventId);
            });
        } catch (Exception e) {
            log.error("Error handling recording_failed webhook", e);
        }
    }

    private UUID extractEventIdFromRoomName(String roomName) {
        try {
            if (roomName.startsWith("event-")) {
                return UUID.fromString(roomName.substring(6));
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
