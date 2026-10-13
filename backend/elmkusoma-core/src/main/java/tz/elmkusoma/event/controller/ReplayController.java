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
@RequestMapping("/v1/replays")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'STUDENT', 'OTHER_LEARNER')")
public class ReplayController {

    private static final Logger log = LoggerFactory.getLogger(ReplayController.class);

    private final ReplayRepository replayRepository;
    private final ReplayProgressRepository progressRepository;
    private final EventRepository eventRepository;
    private final tz.elmkusoma.event.repository.EventMaterialRepository materialRepository;
    private final tz.elmkusoma.course.repository.LiveClassRepository liveClassRepository;
    private final tz.elmkusoma.learning.repository.LessonRepository lessonRepository;
    private final tz.elmkusoma.event.service.ReplayEntitlementService replayEntitlementService;
    private final tz.elmkusoma.liveclass.service.LiveClassRecordingAccessService liveClassRecordingAccessService;
    private final tz.elmkusoma.liveclass.service.RecordingPlaybackService recordingPlaybackService;
    private final tz.elmkusoma.liveclass.service.RecordingPlaybackUrls recordingPlaybackUrls;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Replay>>> getAllReplays(
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId,
            @RequestAttribute(value = "userId", required = false) UUID userId,
            @RequestAttribute(value = "userRole", required = false) String userRole,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        List<Replay> replays;
        // §81/§82: optional Pageable pagination
        if ((page != null || size != null) && institutionId != null) {
            org.springframework.data.domain.Page<Replay> result = replayRepository
                    .findByStatusAndIsDeletedFalseAndInstitutionId("AVAILABLE", institutionId,
                            org.springframework.data.domain.PageRequest.of(
                                    page != null && page >= 0 ? page : 0,
                                    size != null && size > 0 ? size : 20,
                                    org.springframework.data.domain.Sort.by("createdAt").descending()));
            replays = result.getContent();
        } else {
            replays = replayRepository.findByStatusAndIsDeletedFalse("AVAILABLE")
                    .stream()
                    .filter(r -> institutionId == null || institutionId.equals(r.getInstitutionId()))
                    .toList();
        }
        // Institution scope alone is not a learner's entitlement. Staff keep the tenant-wide view
        // they have always had here; a learner sees only what {@code LearnerReplayController}
        // already showed them, so this endpoint cannot be used to read another class's recording
        // metadata by switching away from the learner route. Filtering happens before the count so
        // the header cannot advertise rows the caller will not receive.
        if (isLearnerRole(userRole) && userId != null) {
            replays = replays.stream()
                    .filter(r -> replayEntitlementService.hasEntitlement(r, userId))
                    .toList();
        }
        long total = replays.size();
        Map<UUID, ReplayProgress> progressMap = loadProgress(userId, replays);
        populateRelatedLessonId(replays);
        replays.forEach(r -> enrich(r, userId, progressMap));
        log.info("Replays listed: count={}, institutionId={}", replays.size(), institutionId);
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(total))
                .body(ApiResponse.success("Replays retrieved", replays));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReplay(
            @PathVariable UUID id,
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId,
            @RequestAttribute(value = "userId", required = false) UUID userId,
            @RequestAttribute(value = "userRole", required = false) String userRole) {
        return replayRepository.findById(id)
                .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                .filter(r -> institutionId == null || institutionId.equals(r.getInstitutionId()))
                .map(r -> {
                    // Same rule as the byte path and as /v1/learner/replays: a learner reads a
                    // recording's detail only when they are entitled to it, so knowing an id is
                    // not a way around the entitlement check that guards the recording itself.
                    if (isLearnerRole(userRole)
                            && userId != null
                            && !replayEntitlementService.hasEntitlement(r, userId)) {
                        return ResponseEntity
                                .status(org.springframework.http.HttpStatus.FORBIDDEN)
                                .body(ApiResponse.<Map<String, Object>>error(
                                        "You are not entitled to this recorded session"));
                    }
                    enrich(r, userId, Map.of());
                    log.info("Replay accessed: id={}, eventId={}, status={}", r.getId(), r.getEventId(), r.getStatus());
                    Map<String, Object> detail = new HashMap<>();
                    detail.put("replay", r);
                    // §46/§51/§52: related data derived from the linked Event, not hardcoded empties
                    Event event = r.getEventId() != null ? eventRepository.findById(r.getEventId()).orElse(null) : null;
                    if (event == null && r.getLiveSessionId() != null) {
                        liveClassRepository.findById(r.getLiveSessionId())
                                .ifPresent(lc -> r.setRelatedLessonId(lc.getLessonId()));
                    }
                    populateRelatedLessonTitle(r);
                    detail.put("relatedResources", buildRelatedResources(event));
                    detail.put("upcomingEvents", buildUpcomingEvents(
                            r.getInstitutionId() != null ? r.getInstitutionId()
                                    : (event != null ? event.getInstitutionId() : institutionId),
                            r.getEventId()));
                    detail.put("relatedCourseId", event != null ? event.getRelatedCourseId() : null);
                    detail.put("relatedModuleId", event != null ? event.getRelatedModuleId() : null);
                    detail.put("relatedLessonId", event != null ? event.getRelatedLessonId() : r.getRelatedLessonId());
                    return ResponseEntity.ok(ApiResponse.success("Replay found", detail));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Streams a replay's recording to an authorised caller.
     *
     * <p>Every other replay endpoint returns metadata; this one is the recording
     * <em>access</em> path, so it re-checks the same role, institution and per-replay
     * entitlement rules on every request - including every {@code Range} request a player
     * makes while seeking. A URL that is copied, shared or replayed therefore grants nothing
     * on its own: it is only a pointer at this endpoint, and the caller still has to
     * satisfy the entitlement check with a live session.</p>
     *
     * <p>The stored object location, object key and any signed query are never returned,
     * logged, or used as an input the caller can influence.</p>
     */
    @GetMapping("/{id}/playback")
    public ResponseEntity<org.springframework.core.io.Resource> playReplay(
            @PathVariable UUID id,
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId,
            @RequestAttribute(value = "userId", required = false) UUID userId,
            @RequestAttribute(value = "userRole", required = false) String userRole,
            @RequestHeader(value = "X-Institution-Id", required = false) UUID headerInstitutionId,
            @RequestHeader(value = "Range", required = false) String range) {

        // Same fallback the recording endpoints already use: the resolved institution wins,
        // and the caller's own header is only a fallback. Without it a caller whose
        // membership lookup came back empty would skip the scope filter entirely.
        UUID callerInstitutionId = institutionId != null ? institutionId : headerInstitutionId;

        Replay replay = replayRepository.findById(id)
                .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                .filter(r -> callerInstitutionId == null
                        || callerInstitutionId.equals(r.getInstitutionId()))
                .orElse(null);
        if (replay == null) {
            // Unknown id and foreign-institution id answer identically, so a probe cannot
            // distinguish "does not exist" from "exists somewhere else".
            return ResponseEntity.notFound().build();
        }
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        if (!isReplayPlaybackAllowed(replay, userId, callerInstitutionId, userRole)) {
            return ResponseEntity.status(403).build();
        }

        return streamReplayRecording(replay, range);
    }

    /**
     * Roles whose replay access is decided per recording rather than per tenant.
     *
     * <p>Staff are deliberately excluded: admins read their scope and teachers read the classes
     * they own, which the playback endpoint has always expressed separately. Only a learner's
     * view is entitlement-scoped, and only learner roles are on both sides of that rule.</p>
     */
    private static boolean isLearnerRole(String userRole) {
        return "STUDENT".equals(userRole) || "OTHER_LEARNER".equals(userRole);
    }

    /**
     * Admins read anything in their scope; a teacher reads their own class's recordings and
     * the institution's event replays; everyone else must prove they were in the session or
     * belong to the audience it was recorded for.
     */
    private boolean isReplayPlaybackAllowed(Replay replay, UUID userId, UUID institutionId, String userRole) {
        if ("ADMIN".equals(userRole) || "INSTITUTION_ADMIN".equals(userRole)
                || "NATIONAL_ADMIN".equals(userRole)) {
            return true;
        }
        if ("TEACHER".equals(userRole)) {
            if (replay.getLiveSessionId() == null) {
                // Event replays: the class-level role gate plus institution scope is the
                // rule this endpoint has always applied to teaching staff.
                return true;
            }
            // Live-class replays: reuse the class ownership rule verbatim rather than
            // restating it, so a teacher can never watch another teacher's recording.
            return liveClassRecordingAccessService.isAllowed(
                    liveClassRecordingAccessService.findClass(replay.getLiveSessionId()),
                    userId, institutionId, userRole);
        }
        return replayEntitlementService.hasEntitlement(replay, userId);
    }

    private ResponseEntity<org.springframework.core.io.Resource> streamReplayRecording(Replay replay, String range) {
        String stored = replay.getRecordingUrl();
        // The publisher writes the live class row first, so a replay can legitimately not
        // carry its own location yet. Both are persisted application state, never a client
        // input, and both are re-validated before a single byte is fetched.
        if ((stored == null || stored.isBlank()) && replay.getLiveSessionId() != null) {
            stored = liveClassRepository.findById(replay.getLiveSessionId())
                    .map(tz.elmkusoma.course.domain.LiveClass::getRecordingUrl)
                    .orElse(null);
        }
        java.util.Optional<String> fetchUrl = recordingPlaybackService.resolveFetchUrl(stored);
        if (fetchUrl.isEmpty()) {
            return ResponseEntity.status(stored == null || stored.isBlank() ? 404 : 503).build();
        }
        return recordingPlaybackService.stream(fetchUrl.get(), range)
                .orElseGet(() -> ResponseEntity.status(502).build());
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<ApiResponse<List<Replay>>> getReplaysByEvent(
            @PathVariable UUID eventId,
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId,
            @RequestAttribute(value = "userId", required = false) UUID userId) {
        List<Replay> replays = replayRepository.findByEventIdAndIsDeletedFalse(eventId)
                .stream()
                .filter(r -> institutionId == null || institutionId.equals(r.getInstitutionId()))
                .toList();
        Map<UUID, ReplayProgress> progressMap = loadProgress(userId, replays);
        replays.forEach(r -> enrich(r, userId, progressMap));
        log.info("Replays by event: eventId={}, count={}", eventId, replays.size());
        return ResponseEntity.ok(ApiResponse.success("Replays retrieved", replays));
    }

    @GetMapping("/{id}/progress")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProgress(
            @PathVariable UUID id,
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId,
            @RequestAttribute(value = "userId", required = false) UUID userId) {
        return replayRepository.findById(id)
                .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                .filter(r -> institutionId == null || institutionId.equals(r.getInstitutionId()))
                .map(r -> ResponseEntity.ok(ApiResponse.success("Progress retrieved",
                        buildProgressResponse(r, userId))))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/progress")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateProgress(
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
                    log.info("Replay progress updated: id={}, userId={}, position={}s", id, userId, position);
                    Map<String, Object> data = new HashMap<>();
                    data.put("positionSeconds", position);
                    data.put("completed", completed);
                    return ResponseEntity.ok(ApiResponse.success("Progress updated", data));
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

    /** Lesson ↔ Live Class: live-class replays derive their related lesson from live_classes.lesson_id. */
    private void populateRelatedLessonId(List<Replay> replays) {
        List<UUID> sessionIds = replays.stream()
                .filter(r -> r.getRelatedLessonId() == null && r.getLiveSessionId() != null)
                .map(Replay::getLiveSessionId)
                .distinct()
                .toList();
        if (sessionIds.isEmpty()) {
            return;
        }
        Map<UUID, UUID> lessonBySession = new HashMap<>();
        liveClassRepository.findAllById(sessionIds)
                .forEach(lc -> lessonBySession.put(lc.getId(), lc.getLessonId()));
        Map<UUID, String> lessonTitles = new HashMap<>();
        replays.forEach(r -> {
            if (r.getRelatedLessonId() == null && r.getLiveSessionId() != null) {
                UUID lessonId = lessonBySession.get(r.getLiveSessionId());
                if (lessonId != null) {
                    r.setRelatedLessonId(lessonId);
                    if (r.getRelatedLessonTitle() == null) {
                        r.setRelatedLessonTitle(lessonTitles.computeIfAbsent(lessonId,
                                id -> lessonRepository.findById(id).map(l -> l.getTitle()).orElse(null)));
                    }
                }
            }
        });
    }

    private void populateRelatedLessonTitle(Replay r) {
        if (r.getRelatedLessonId() == null || r.getRelatedLessonTitle() != null) {
            return;
        }
        r.setRelatedLessonTitle(lessonRepository.findById(r.getRelatedLessonId())
                .map(l -> l.getTitle()).orElse(null));
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
        // Playback URL, not the storage location. The player points <video src> here, so the
        // browser fetches through the entitlement-checked playback endpoint instead of
        // straight at a private bucket that would otherwise 403 - or, if LiveKit ever returns
        // a signed location, hand out a bearer capability to anyone who saw the JSON.
        r.setVideoUrl(recordingPlaybackUrls.forReplay(r.getId()));
        // No caption track is ever generated by the recording pipeline, so captionUrl stays
        // null instead of being faked with a sample file the learner would see as real captions.
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

    /** §46/§51/§52: related resources from the linked event's related ids + materials. */
    private List<Map<String, Object>> buildRelatedResources(Event event) {
        List<Map<String, Object>> related = new java.util.ArrayList<>();
        if (event == null) {
            return related;
        }
        addRelatedIfPresent(related, "COURSE", event.getRelatedCourseId());
        addRelatedIfPresent(related, "MODULE", event.getRelatedModuleId());
        addRelatedIfPresent(related, "LESSON", event.getRelatedLessonId());
        materialRepository.findByEventIdAndIsPublicTrueAndIsDeletedFalseOrderBySortOrderAsc(event.getId())
                .forEach(m -> {
                    Map<String, Object> res = new HashMap<>();
                    res.put("type", "MATERIAL");
                    res.put("id", m.getId().toString());
                    res.put("title", m.getTitle());
                    res.put("url", m.getFileUrl());
                    res.put("materialType", m.getMaterialType());
                    related.add(res);
                });
        return related;
    }

    private void addRelatedIfPresent(List<Map<String, Object>> related, String type, String id) {
        if (id == null || id.isBlank()) {
            return;
        }
        Map<String, Object> res = new HashMap<>();
        res.put("type", type);
        res.put("id", id);
        related.add(res);
    }

    /** §46/§52: upcoming events for the same institution, excluding the replay's own event. */
    private List<Map<String, Object>> buildUpcomingEvents(UUID institutionId, UUID excludeEventId) {
        if (institutionId == null) {
            return List.of();
        }
        return eventRepository.findUpcomingPublished(institutionId, java.time.LocalDateTime.now())
                .stream()
                .filter(e -> excludeEventId == null || !excludeEventId.equals(e.getId()))
                .limit(5)
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", e.getId().toString());
                    m.put("title", e.getTitle());
                    m.put("startsAt", e.getStartsAt());
                    m.put("eventType", e.getEventType());
                    return m;
                })
                .collect(java.util.stream.Collectors.toList());
    }
}
