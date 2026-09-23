package tz.elmkusoma.event.controller;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.event.domain.Event;
import tz.elmkusoma.event.domain.Replay;
import tz.elmkusoma.event.domain.ReplayProgress;
import tz.elmkusoma.event.repository.EventRepository;
import tz.elmkusoma.event.repository.ReplayProgressRepository;
import tz.elmkusoma.event.repository.ReplayRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/learner/replays")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN')")
public class LearnerReplayController {

    private static final Logger log = LoggerFactory.getLogger(LearnerReplayController.class);

    private final ReplayRepository replayRepository;
    private final ReplayProgressRepository progressRepository;
    private final EventRepository eventRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Replay>>> getReplays(
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId,
            @RequestAttribute(value = "userId", required = false) UUID userId) {
        List<Replay> replays = replayRepository.findByStatusAndIsDeletedFalse("AVAILABLE")
                .stream()
                .filter(r -> institutionId == null || institutionId.equals(r.getInstitutionId()))
                .toList();
        Map<UUID, ReplayProgress> progressMap = loadProgress(userId, replays);
        replays.forEach(r -> enrich(r, userId, progressMap));
        return ResponseEntity.ok(ApiResponse.success(replays));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReplay(
            @PathVariable UUID id,
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId,
            @RequestAttribute(value = "userId", required = false) UUID userId) {
        return replayRepository.findById(id)
                .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                .filter(r -> institutionId == null || institutionId.equals(r.getInstitutionId()))
                .map(r -> {
                    r.setViewCount(r.getViewCount() + 1);
                    replayRepository.save(r);
                    enrich(r, userId, Map.of());
                    Map<String, Object> detail = new HashMap<>();
                    detail.put("replay", r);
                    detail.put("relatedResources", List.of());
                    detail.put("upcomingEvents", List.of());
                    return ResponseEntity.ok(ApiResponse.success(detail));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/progress")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReplayProgress(
            @PathVariable UUID id,
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId,
            @RequestAttribute(value = "userId", required = false) UUID userId) {
        return replayRepository.findById(id)
                .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                .filter(r -> institutionId == null || institutionId.equals(r.getInstitutionId()))
                .map(r -> ResponseEntity.ok(ApiResponse.success(buildProgressResponse(r, userId))))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/progress")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateReplayProgress(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body,
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId,
            @RequestAttribute(value = "userId", required = false) UUID userId) {
        if (userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Authentication required"));
        }
        return replayRepository.findById(id)
                .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                .filter(r -> institutionId == null || institutionId.equals(r.getInstitutionId()))
                .map(replay -> {
                    int position = extractPosition(body);
                    boolean completed = extractCompleted(body, replay, position);
                    upsertProgress(replay, userId, position, completed);
                    replayRepository.save(replay);
                    Map<String, Object> data = new HashMap<>();
                    data.put("positionSeconds", position);
                    data.put("completed", completed);
                    return ResponseEntity.ok(ApiResponse.success(data));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private static int extractPosition(Map<String, Object> body) {
        Object raw = body.containsKey("positionSeconds")
                ? body.get("positionSeconds")
                : body.getOrDefault("position", 0);
        return raw instanceof Number n ? n.intValue() : 0;
    }

    private static boolean extractCompleted(Map<String, Object> body, Replay replay, int position) {
        Object raw = body.get("completed");
        if (raw instanceof Boolean b) {
            return b;
        }
        if (position <= 0) {
            return false;
        }
        Integer duration = replay.getDurationSeconds();
        return duration != null && duration > 0 && position >= duration - 10;
    }

    private void upsertProgress(Replay replay, UUID userId, int position, boolean completed) {
        ReplayProgress progress = progressRepository
                .findByReplayIdAndUserId(replay.getId(), userId)
                .orElseGet(() -> ReplayProgress.builder()
                        .replayId(replay.getId())
                        .userId(userId)
                        .build());
        progress.setPositionSeconds(position);
        progress.setCompleted(completed);
        progressRepository.save(progress);
        replay.setLastPositionSeconds(position);
    }

    private Map<String, Object> buildProgressResponse(Replay replay, UUID userId) {
        Map<String, Object> data = new HashMap<>();
        int position = 0;
        boolean completed = false;
        if (userId != null) {
            ReplayProgress progress = progressRepository
                    .findByReplayIdAndUserId(replay.getId(), userId).orElse(null);
            if (progress != null) {
                position = progress.getPositionSeconds() != null ? progress.getPositionSeconds() : 0;
                completed = Boolean.TRUE.equals(progress.getCompleted());
                if (progress.getUpdatedAt() != null) {
                    data.put("lastWatchedAt", progress.getUpdatedAt().toString());
                }
            }
        } else if (replay.getLastPositionSeconds() != null) {
            position = replay.getLastPositionSeconds();
        }
        data.put("positionSeconds", position);
        data.put("completed", completed);
        data.put("viewCount", replay.getViewCount() != null ? replay.getViewCount() : 0);
        return data;
    }

    private Map<UUID, ReplayProgress> loadProgress(UUID userId, List<Replay> replays) {
        if (userId == null || replays.isEmpty()) {
            return Map.of();
        }
        List<UUID> ids = replays.stream().map(Replay::getId).toList();
        Map<UUID, ReplayProgress> map = new HashMap<>();
        progressRepository.findByUserIdAndReplayIdIn(userId, ids).forEach(p -> map.put(p.getReplayId(), p));
        return map;
    }

    private void enrich(Replay r, UUID userId, Map<UUID, ReplayProgress> progressMap) {
        ReplayProgress p = userId != null ? progressMap.get(r.getId()) : null;
        if (p != null) {
            r.setPositionSeconds(p.getPositionSeconds() != null ? p.getPositionSeconds() : 0);
            r.setCompleted(Boolean.TRUE.equals(p.getCompleted()));
        } else if (userId == null) {
            r.setPositionSeconds(r.getLastPositionSeconds() != null ? r.getLastPositionSeconds() : 0);
            r.setCompleted(false);
        } else {
            r.setPositionSeconds(0);
            r.setCompleted(false);
        }
        r.setVideoUrl(r.getRecordingUrl());
        r.setRecordedAt(r.getCreatedAt());
        if (r.getEventId() != null && (r.getEventTitle() == null || r.getPresenterName() == null)) {
            Event event = eventRepository.findById(r.getEventId()).orElse(null);
            if (event != null) {
                if (r.getEventTitle() == null) {
                    r.setEventTitle(event.getTitle());
                }
                if (r.getPresenterName() == null) {
                    r.setPresenterName(event.getPresenterName());
                }
            }
        }
    }
}
