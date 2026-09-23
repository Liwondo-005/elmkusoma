package tz.elmkusoma.liveclass.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.event.domain.Event;
import tz.elmkusoma.event.domain.EventRegistration;
import tz.elmkusoma.event.domain.EventStatus;
import tz.elmkusoma.event.domain.Replay;
import tz.elmkusoma.event.repository.EventRegistrationRepository;
import tz.elmkusoma.event.repository.EventRepository;
import tz.elmkusoma.event.repository.ReplayRepository;
import tz.elmkusoma.liveclass.config.LiveKitConfig;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/v1/webhooks/livekit")
@RequiredArgsConstructor
@Slf4j
public class LiveKitWebhookController {

    private static final Set<String> PRE_LIVE_STATUSES = Set.of(
            "PUBLISHED", "REGISTRATION_OPEN", "REGISTRATION_CLOSED", "PREPARING", "STARTING");
    private static final Set<EventStatus> PRE_LIVE_EVENT_STATUSES = Set.of(
            EventStatus.PUBLISHED, EventStatus.REGISTRATION_OPEN, EventStatus.REGISTRATION_CLOSED,
            EventStatus.PREPARING, EventStatus.STARTING, EventStatus.RECORDING, EventStatus.PROCESSING);
    private static final Set<String> LIVE_LIKE_STATUSES = Set.of("LIVE", "ENDING", "STARTING", "PREPARING");
    private static final Set<EventStatus> LIVE_LIKE_EVENT_STATUSES = Set.of(
            EventStatus.LIVE, EventStatus.ENDING, EventStatus.STARTING, EventStatus.PREPARING);

    private final EventRepository eventRepository;
    private final ReplayRepository replayRepository;
    private final EventRegistrationRepository registrationRepository;
    private final LiveKitConfig liveKitConfig;
    private final Environment environment;
    private final UserRepository userRepository;
    private final tz.elmkusoma.administration.service.PlatformIntegrationService integrationService;
    private final ObjectMapper objectMapper;

    private final Set<String> processedWebhookKeys = ConcurrentHashMap.newKeySet();

    @PostMapping
    public ResponseEntity<Map<String, String>> handleWebhook(
            @RequestBody byte[] rawBody,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        String verificationStatus = verifyWebhookSignature(authHeader, rawBody);
        if ("FAILED".equals(verificationStatus)) {
            log.warn("LiveKit webhook rejected: signature verification failed");
            integrationService.recordWebhook("LIVEKIT", "rejected", "FAILED", "FAILED",
                    "Invalid webhook signature");
            return ResponseEntity.status(401).body(Map.of("error", "Invalid webhook signature"));
        }

        Map<String, Object> payload;
        try {
            payload = objectMapper.readValue(rawBody, Map.class);
        } catch (Exception e) {
            integrationService.recordWebhook("LIVEKIT", "malformed", verificationStatus, "FAILED",
                    "Malformed JSON body");
            return ResponseEntity.badRequest().body(Map.of("error", "Malformed JSON body"));
        }

        String event = payload.get("event") instanceof String s ? s : null;
        log.info("LiveKit webhook received: event={}, verification={}", event, verificationStatus);

        if (event == null) {
            integrationService.recordWebhook("LIVEKIT", "malformed", verificationStatus, "FAILED",
                    "Missing event field");
            return ResponseEntity.badRequest().body(Map.of("error", "Missing event field"));
        }

        try {
            if (!firstDelivery(event, payload)) {
                log.debug("Duplicate LiveKit webhook ignored: event={}", event);
                integrationService.recordWebhook("LIVEKIT", event, verificationStatus, "SUCCESS",
                        "Duplicate delivery ignored");
                return ResponseEntity.ok(Map.of("status", "ok"));
            }

            switch (event) {
                case "room_started" -> handleRoomStarted(payload);
                case "room_ended" -> handleRoomEnded(payload);
                case "participant_joined" -> handleParticipantEvent(payload, event);
                case "participant_left" -> handleParticipantEvent(payload, event);
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
            integrationService.recordWebhook("LIVEKIT", event, verificationStatus, "SUCCESS", null);
        } catch (Exception e) {
            log.error("Error processing LiveKit webhook: event={}", event, e);
            integrationService.recordWebhook("LIVEKIT", event, verificationStatus, "FAILED", e.getMessage());
        }

        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    // ==================== Signature verification (§63) ====================

    /**
     * LiveKit signs the Authorization header with a JWT (HS256) whose payload carries a
     * "sha256" claim: the lowercase hex SHA-256 of the raw body bytes, signed with the
     * API secret. Verified with raw JCA Mac so short dev secrets (devsecret) work —
     * jjwt enforces a 256-bit minimum and rejects them.
     */
    private String verifyWebhookSignature(String authHeader, byte[] rawBody) {
        String apiSecret = liveKitConfig.getServer().getApiSecret();
        if (apiSecret == null || apiSecret.isBlank()) {
            if (environment.matchesProfiles("prod", "production")) {
                log.error("LiveKit webhook rejected: no api-secret configured in production");
                return "FAILED";
            }
            return "UNVERIFIED";
        }
        try {
            if (authHeader == null || authHeader.isBlank()) {
                return "FAILED";
            }
            String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7).trim() : authHeader.trim();
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return "FAILED";
            }

            byte[] signingInput = (parts[0] + "." + parts[1]).getBytes(StandardCharsets.UTF_8);
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expectedSig = mac.doFinal(signingInput);
            byte[] providedSig = Base64.getUrlDecoder().decode(parts[2]);
            if (!MessageDigest.isEqual(expectedSig, providedSig)) {
                return "FAILED";
            }

            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            @SuppressWarnings("unchecked")
            Map<String, Object> claims = objectMapper.readValue(payloadJson, Map.class);

            String apiKey = liveKitConfig.getServer().getApiKey();
            if (apiKey != null && !apiKey.isBlank()) {
                Object iss = claims.get("iss");
                if (iss == null || !apiKey.equals(iss.toString())) {
                    log.warn("LiveKit webhook issuer mismatch: iss={}", iss);
                    return "FAILED";
                }
            }

            String providedHash = claims.get("sha256") instanceof String s ? s : null;
            if (providedHash == null) {
                return "FAILED";
            }
            String actualHash = sha256Hex(rawBody);
            if (!MessageDigest.isEqual(
                    actualHash.getBytes(StandardCharsets.UTF_8),
                    providedHash.toLowerCase().getBytes(StandardCharsets.UTF_8))) {
                log.warn("LiveKit webhook body hash mismatch");
                return "FAILED";
            }
            return "VERIFIED";
        } catch (Exception e) {
            log.warn("LiveKit webhook signature verification failed: {}", e.getMessage());
            return "FAILED";
        }
    }

    private static String sha256Hex(byte[] data) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(data);
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

    // ==================== Idempotency / duplicate guard (§34) ====================

    private boolean firstDelivery(String event, Map<String, Object> payload) {
        String roomKey = "n/a";
        if (payload.get("room") instanceof Map<?, ?> room) {
            Object sid = room.get("sid") != null ? room.get("sid") : room.get("name");
            roomKey = sid != null ? sid.toString() : "n/a";
        }
        String who = "";
        if (payload.get("participant") instanceof Map<?, ?> participant) {
            Object id = participant.get("identity") != null ? participant.get("identity") : participant.get("id");
            who = id != null ? id.toString() : "";
        }
        String egressId = "";
        if (payload.get("egress") instanceof Map<?, ?> egress && egress.get("id") != null) {
            egressId = egress.get("id").toString();
        }
        String key = event + "|" + roomKey + "|" + who + "|" + egressId;
        if (processedWebhookKeys.size() > 50_000) {
            processedWebhookKeys.clear();
        }
        return processedWebhookKeys.add(key);
    }

    // ==================== Room → event mapping ====================

    /**
     * Resolves the Event a LiveKit room belongs to:
     * 1. room name convention "event-&lt;eventId&gt;" (set by LiveKitService.generateRoomNameForEvent)
     * 2. room.metadata JSON containing {"eventId": "..."} (stored when creating event rooms)
     * 3. egress.roomName fallback for recording events
     * Live-class rooms ("liveclass-&lt;classId&gt;") have no event link — returns null there.
     */
    private UUID resolveEventId(Map<String, Object> payload) {
        UUID fromRoom = resolveEventIdFromRoom(payload.get("room"));
        if (fromRoom != null) {
            return fromRoom;
        }
        if (payload.get("egress") instanceof Map<?, ?> egress && egress.get("roomName") != null) {
            return extractEventIdFromRoomName(egress.get("roomName").toString());
        }
        return null;
    }

    private UUID resolveEventIdFromRoom(Object roomObj) {
        if (!(roomObj instanceof Map<?, ?> room)) {
            return null;
        }
        if (room.get("name") != null) {
            UUID fromName = extractEventIdFromRoomName(room.get("name").toString());
            if (fromName != null) {
                return fromName;
            }
        }
        return extractEventIdFromMetadata(room.get("metadata"));
    }

    private UUID extractEventIdFromRoomName(String roomName) {
        try {
            if (roomName == null || !roomName.startsWith("event-")) {
                return null;
            }
            String rest = roomName.substring("event-".length());
            if (rest.length() >= 36) {
                return UUID.fromString(rest.substring(0, 36));
            }
            return UUID.fromString(rest);
        } catch (Exception e) {
            return null;
        }
    }

    private UUID extractEventIdFromMetadata(Object metadata) {
        if (metadata == null) {
            return null;
        }
        String meta = metadata.toString();
        if (meta.isBlank()) {
            return null;
        }
        String trimmed = meta.trim();
        if (trimmed.length() == 36) {
            try {
                return UUID.fromString(trimmed);
            } catch (IllegalArgumentException ignored) {
                // fall through to JSON parsing
            }
        }
        try {
            com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(meta);
            com.fasterxml.jackson.databind.JsonNode idNode = node.get("eventId");
            if (idNode != null && !idNode.isNull()) {
                return UUID.fromString(idNode.asText());
            }
        } catch (Exception ignored) {
            // not JSON — no metadata mapping
        }
        return null;
    }

    // ==================== room_started / room_ended → event status (§63) ====================

    private void handleRoomStarted(Map<String, Object> payload) {
        try {
            UUID eventId = resolveEventId(payload);
            if (eventId == null) {
                log.debug("room_started with no event mapping (room={})", payload.get("room"));
                return;
            }
            eventRepository.findById(eventId).ifPresent(event -> {
                String status = event.getStatus();
                EventStatus eventStatus = event.getEventStatus();
                if ("LIVE".equals(status) || eventStatus == EventStatus.LIVE) {
                    return;
                }
                boolean canGoLive = PRE_LIVE_STATUSES.contains(status)
                        || (eventStatus != null && PRE_LIVE_EVENT_STATUSES.contains(eventStatus));
                if (!canGoLive) {
                    log.debug("room_started ignored for event {} in status {}/{}", eventId, status, eventStatus);
                    return;
                }
                event.setStatus("LIVE");
                event.setEventStatus(EventStatus.LIVE);
                eventRepository.save(event);
                log.info("Event set LIVE from LiveKit room_started: eventId={}", eventId);
            });
        } catch (Exception e) {
            log.error("Error handling room_started webhook", e);
        }
    }

    private void handleRoomEnded(Map<String, Object> payload) {
        try {
            UUID eventId = resolveEventId(payload);
            if (eventId == null) {
                log.debug("room_ended with no event mapping (room={})", payload.get("room"));
                return;
            }
            eventRepository.findById(eventId).ifPresent(event -> {
                String status = event.getStatus();
                EventStatus eventStatus = event.getEventStatus();
                if ("ENDED".equals(status) || eventStatus == EventStatus.ENDED) {
                    return;
                }
                boolean canEnd = LIVE_LIKE_STATUSES.contains(status)
                        || (eventStatus != null && LIVE_LIKE_EVENT_STATUSES.contains(eventStatus));
                if (!canEnd) {
                    log.debug("room_ended ignored for event {} in status {}/{}", eventId, status, eventStatus);
                    return;
                }
                event.setStatus("ENDED");
                event.setEventStatus(EventStatus.ENDED);
                eventRepository.save(event);
                log.info("Event set ENDED from LiveKit room_ended: eventId={}", eventId);
            });
        } catch (Exception e) {
            log.error("Error handling room_ended webhook", e);
        }
    }

    // ==================== participant_joined/left → attendance (§33) ====================

    private void handleParticipantEvent(Map<String, Object> payload, String eventType) {
        try {
            UUID eventId = resolveEventId(payload);
            if (eventId == null) {
                log.debug("{} with no event mapping (room={})", eventType, payload.get("room"));
                return;
            }
            UUID userId = extractUserId(payload);
            if (userId == null) {
                log.debug("{}: could not resolve user identity from payload", eventType);
                return;
            }
            markEventAttendance(eventId, userId);
        } catch (Exception e) {
            log.error("Error handling {} webhook", eventType, e);
        }
    }

    private UUID extractUserId(Map<String, Object> payload) {
        if (!(payload.get("participant") instanceof Map<?, ?> participant)) {
            return null;
        }
        Object identityObj = participant.get("identity");
        if (identityObj == null) {
            return null;
        }
        String identity = identityObj.toString();
        try {
            return UUID.fromString(identity);
        } catch (IllegalArgumentException notUuid) {
            // identity may be an email (token generation uses userId, but tolerate emails)
        }
        if (identity.contains("@")) {
            try {
                User user = userRepository.findByEmailAndIsDeletedFalse(identity).orElse(null);
                if (user != null) {
                    return user.getId();
                }
            } catch (Exception e) {
                log.debug("Could not resolve identity {} to a user", identity);
            }
        }
        return null;
    }

    /** Idempotent: marks (or creates) event attendance for the user. */
    private void markEventAttendance(UUID eventId, UUID userId) {
        Event event = eventRepository.findById(eventId).orElse(null);
        if (event == null) {
            return;
        }
        EventRegistration registration = registrationRepository
                .findByEventIdAndUserIdAndIsDeletedFalse(eventId, userId).orElse(null);
        if (registration == null) {
            registration = EventRegistration.builder()
                    .eventId(eventId)
                    .userId(userId)
                    .institutionId(event.getInstitutionId())
                    .status("ATTENDED")
                    .registeredAt(LocalDateTime.now())
                    .attended(true)
                    .attendedAt(LocalDateTime.now())
                    .build();
            registrationRepository.save(registration);
            log.info("Event attendance recorded (walk-in): eventId={}, userId={}", eventId, userId);
            return;
        }
        if (Boolean.TRUE.equals(registration.getAttended())) {
            return;
        }
        if ("CANCELLED".equals(registration.getStatus())) {
            return;
        }
        registration.setAttended(true);
        registration.setAttendedAt(LocalDateTime.now());
        if ("REGISTERED".equals(registration.getStatus()) || "WAITLISTED".equals(registration.getStatus())) {
            registration.setStatus("ATTENDED");
        }
        registrationRepository.save(registration);
        log.info("Event attendance marked: eventId={}, userId={}", eventId, userId);
    }

    // ==================== Recording lifecycle → Replay (§45) ====================

    private void handleRecordingStarted(Map<String, Object> payload) {
        try {
            UUID eventId = resolveEventId(payload);
            if (eventId == null) return;

            eventRepository.findById(eventId).ifPresent(event -> {
                event.setRecordingStatus("PROCESSING");
                eventRepository.save(event);
                log.info("Event recording status set to PROCESSING: eventId={}", eventId);

                List<Replay> replays = replayRepository.findByEventIdAndIsDeletedFalse(eventId);
                boolean hasProcessing = replays.stream()
                        .anyMatch(r -> Replay.STATUS_PROCESSING.equals(r.getStatus()));
                if (!hasProcessing) {
                    Replay replay = Replay.builder()
                            .eventId(eventId)
                            .institutionId(event.getInstitutionId())
                            .title(event.getTitle())
                            .description("Replay of " + event.getTitle())
                            .status(Replay.STATUS_PROCESSING)
                            .viewCount(0)
                            .lastPositionSeconds(0)
                            .build();
                    replayRepository.save(replay);
                    log.info("Replay PROCESSING created from recording_started: eventId={}, replayId={}",
                            eventId, replay.getId());
                }
            });
        } catch (Exception e) {
            log.error("Error handling recording_started webhook", e);
        }
    }

    private void handleRecordingCompleted(Map<String, Object> payload) {
        try {
            UUID eventId = resolveEventId(payload);
            if (eventId == null) return;

            String recordingUrl = extractRecordingUrl(payload);
            Integer durationSeconds = extractDurationSeconds(payload);

            eventRepository.findById(eventId).ifPresent(event -> {
                event.setRecordingStatus("AVAILABLE");
                if (recordingUrl != null) {
                    event.setRecordingUrl(recordingUrl);
                }
                eventRepository.save(event);

                List<Replay> replays = replayRepository.findByEventIdAndIsDeletedFalse(eventId);

                Replay target = replays.stream()
                        .filter(r -> Replay.STATUS_PROCESSING.equals(r.getStatus()))
                        .findFirst()
                        .orElse(null);

                if (target == null && replays.stream().noneMatch(r -> Replay.STATUS_AVAILABLE.equals(r.getStatus()))) {
                    target = Replay.builder()
                            .eventId(eventId)
                            .institutionId(event.getInstitutionId())
                            .title(event.getTitle())
                            .description("Replay of " + event.getTitle())
                            .viewCount(0)
                            .lastPositionSeconds(0)
                            .build();
                }

                if (target != null) {
                    target.setStatus(Replay.STATUS_AVAILABLE);
                    if (recordingUrl != null) {
                        target.setRecordingUrl(recordingUrl);
                    }
                    if (durationSeconds != null) {
                        target.setDurationSeconds(durationSeconds);
                    }
                    replayRepository.save(target);
                    log.info("Replay AVAILABLE from recording_completed: eventId={}, replayId={}",
                            eventId, target.getId());
                } else {
                    replays.stream()
                            .filter(r -> Replay.STATUS_AVAILABLE.equals(r.getStatus()))
                            .filter(r -> recordingUrl != null && !recordingUrl.equals(r.getRecordingUrl()))
                            .findFirst()
                            .ifPresent(r -> {
                                r.setRecordingUrl(recordingUrl);
                                if (durationSeconds != null) {
                                    r.setDurationSeconds(durationSeconds);
                                }
                                replayRepository.save(r);
                            });
                }
            });
        } catch (Exception e) {
            log.error("Error handling recording_completed webhook", e);
        }
    }

    private void handleRecordingFailed(Map<String, Object> payload) {
        try {
            UUID eventId = resolveEventId(payload);
            if (eventId == null) return;

            eventRepository.findById(eventId).ifPresent(event -> {
                event.setRecordingStatus("FAILED");
                eventRepository.save(event);
                log.info("Event recording status set to FAILED: eventId={}", eventId);

                List<Replay> replays = replayRepository.findByEventIdAndIsDeletedFalse(eventId);
                Replay target = replays.stream()
                        .filter(r -> Replay.STATUS_PROCESSING.equals(r.getStatus()))
                        .findFirst()
                        .orElse(null);
                if (target == null && replays.isEmpty()) {
                    target = Replay.builder()
                            .eventId(eventId)
                            .institutionId(event.getInstitutionId())
                            .title(event.getTitle())
                            .description("Replay of " + event.getTitle())
                            .viewCount(0)
                            .lastPositionSeconds(0)
                            .build();
                }
                if (target != null) {
                    target.setStatus(Replay.STATUS_FAILED);
                    replayRepository.save(target);
                    log.info("Replay FAILED from recording_failed: eventId={}, replayId={}",
                            eventId, target.getId());
                }
            });
        } catch (Exception e) {
            log.error("Error handling recording_failed webhook", e);
        }
    }

    private String extractRecordingUrl(Map<String, Object> payload) {
        if (!(payload.get("egress") instanceof Map<?, ?> egress)) {
            return null;
        }
        Object fileResults = egress.get("fileResults");
        if (fileResults instanceof List<?> files && !files.isEmpty()
                && files.get(0) instanceof Map<?, ?> firstFile) {
            Object url = firstFile.get("url");
            if (url != null) {
                return url.toString();
            }
        }
        return null;
    }

    private Integer extractDurationSeconds(Map<String, Object> payload) {
        if (!(payload.get("egress") instanceof Map<?, ?> egress)) {
            return null;
        }
        Object fileResults = egress.get("fileResults");
        if (fileResults instanceof List<?> files && !files.isEmpty()
                && files.get(0) instanceof Map<?, ?> firstFile) {
            Object duration = firstFile.get("duration");
            if (duration instanceof Number n) {
                return n.intValue();
            }
        }
        return null;
    }
}
