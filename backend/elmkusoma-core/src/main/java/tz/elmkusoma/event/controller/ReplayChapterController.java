package tz.elmkusoma.event.controller;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.course.domain.LiveClass;
import tz.elmkusoma.course.repository.LiveClassRepository;
import tz.elmkusoma.event.domain.Replay;
import tz.elmkusoma.event.domain.ReplayChapter;
import tz.elmkusoma.event.repository.ReplayChapterRepository;
import tz.elmkusoma.event.repository.ReplayRepository;
import tz.elmkusoma.teacher.domain.Teacher;
import tz.elmkusoma.teacher.repository.TeacherRepository;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Chapter markers on a replay recording.
 *
 * <p>Writing is restricted to the people who can be held responsible for the recording's
 * content: institution staff for anything in their tenant, and a teacher only for replays
 * that came out of their own live classes. A teacher must not be able to chapter someone
 * else's recording, and nobody may cross tenants.</p>
 *
 * <p>Reading goes through {@link LearnerReplayController}, which already applies replay
 * entitlement. It is deliberately not re-implemented here, so there is one answer to "who
 * may see this recording" rather than two that can drift.</p>
 */
@RestController
@RequestMapping("/v1/replays/{replayId}/chapters")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER', 'INSTITUTION_ADMIN', 'ADMIN', 'NATIONAL_ADMIN')")
public class ReplayChapterController {

    private static final Logger log = LoggerFactory.getLogger(ReplayChapterController.class);

    /** Guards a runaway client: a recording of a few hours needs far fewer markers than this. */
    private static final int MAX_CHAPTERS_PER_REPLAY = 100;
    private static final int MAX_TITLE_LENGTH = 200;

    private final ReplayRepository replayRepository;
    private final ReplayChapterRepository chapterRepository;
    private final LiveClassRepository liveClassRepository;
    private final TeacherRepository teacherRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ReplayChapter>>> listChapters(
            @PathVariable UUID replayId,
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId,
            @RequestAttribute(value = "userId") UUID userId,
            @RequestAttribute(value = "userRole", required = false) String userRole) {
        return findManageableReplay(replayId, institutionId, userId, userRole)
                .map(replay -> ResponseEntity.ok(ApiResponse.success("Chapters retrieved",
                        chapterRepository.findByReplayIdAndIsDeletedFalseOrderByPositionSecondsAsc(replay.getId()))))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReplayChapter>> createChapter(
            @PathVariable UUID replayId,
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId,
            @RequestAttribute(value = "userId") UUID userId,
            @RequestAttribute(value = "userRole", required = false) String userRole,
            @RequestBody Map<String, Object> body) {

        Replay replay = findManageableReplay(replayId, institutionId, userId, userRole).orElse(null);
        if (replay == null) {
            return ResponseEntity.notFound().build();
        }

        String title = asText(body.get("title"));
        Integer position = asInteger(body.get("positionSeconds"));
        if (title == null || title.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("A chapter needs a title"));
        }
        title = title.trim();
        if (title.length() > MAX_TITLE_LENGTH) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("A chapter title may not exceed " + MAX_TITLE_LENGTH + " characters"));
        }
        if (position == null || position < 0) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("positionSeconds must be a whole number of seconds from the start"));
        }
        // A marker past the end of the recording can never be reached by the player.
        if (replay.getDurationSeconds() != null && position > replay.getDurationSeconds()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(
                    "positionSeconds is past the end of this recording (" + replay.getDurationSeconds() + "s)"));
        }
        if (chapterRepository.countByReplayIdAndIsDeletedFalse(replay.getId()) >= MAX_CHAPTERS_PER_REPLAY) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("This recording already has " + MAX_CHAPTERS_PER_REPLAY + " chapters"));
        }
        if (chapterRepository.findByReplayIdAndPositionSecondsAndIsDeletedFalse(replay.getId(), position).isPresent()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("A chapter already exists at that timestamp"));
        }

        ReplayChapter chapter = ReplayChapter.builder()
                .replayId(replay.getId())
                .title(title)
                .positionSeconds(position)
                .build();
        chapter.setInstitutionId(replay.getInstitutionId());
        try {
            chapter = chapterRepository.saveAndFlush(chapter);
        } catch (DataIntegrityViolationException e) {
            // Two markers at the same second are indistinguishable when clicked; V151 makes
            // that impossible in the database, so a lost race is reported as a duplicate.
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("A chapter already exists at that timestamp"));
        }
        log.info("Replay chapter created: replayId={}, chapterId={}, positionSeconds={}",
                replay.getId(), chapter.getId(), position);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(ApiResponse.success("Chapter added", chapter));
    }

    @DeleteMapping("/{chapterId}")
    public ResponseEntity<ApiResponse<Void>> deleteChapter(
            @PathVariable UUID replayId,
            @PathVariable UUID chapterId,
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId,
            @RequestAttribute(value = "userId") UUID userId,
            @RequestAttribute(value = "userRole", required = false) String userRole) {

        Replay replay = findManageableReplay(replayId, institutionId, userId, userRole).orElse(null);
        if (replay == null) {
            return ResponseEntity.notFound().build();
        }

        ReplayChapter chapter = chapterRepository.findById(chapterId)
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .filter(c -> replay.getId().equals(c.getReplayId()))
                // Same tenant as the replay, never merely "exists somewhere".
                .filter(c -> replay.getInstitutionId() == null
                        || Objects.equals(replay.getInstitutionId(), c.getInstitutionId()))
                .orElse(null);
        if (chapter == null) {
            return ResponseEntity.notFound().build();
        }

        // Soft delete so the unique (replay, position) key is freed for a legitimate
        // replacement rather than permanently blocking that timestamp.
        chapter.setIsDeleted(true);
        chapterRepository.save(chapter);
        log.info("Replay chapter deleted: replayId={}, chapterId={}", replayId, chapterId);
        return ResponseEntity.ok(ApiResponse.success("Chapter removed", null));
    }

    /**
     * Resolves the replay only if the caller may manage it, so every handler has one
     * authoritative answer rather than a check that can be forgotten on a new endpoint.
     */
    private java.util.Optional<Replay> findManageableReplay(UUID replayId, UUID institutionId,
                                                            UUID userId, String userRole) {
        return replayRepository.findById(replayId)
                .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                // Tenant boundary first: a recording never crosses institutions.
                .filter(r -> institutionId == null || institutionId.equals(r.getInstitutionId()))
                .filter(r -> mayManage(r, userId, userRole));
    }

    private boolean mayManage(Replay replay, UUID userId, String userRole) {
        if (userId == null) {
            return false;
        }
        if ("ADMIN".equals(userRole) || "NATIONAL_ADMIN".equals(userRole)
                || "INSTITUTION_ADMIN".equals(userRole)) {
            return true;
        }
        if (!"TEACHER".equals(userRole)) {
            return false;
        }
        // A teacher may only chapter a recording that came out of their own live class.
        // LiveClass.teacherId is a teachers.id, so it has to be resolved to the users.id
        // that media_assets.teacher_id and the session identity actually use.
        if (replay.getLiveSessionId() == null) {
            return false;
        }
        return liveClassRepository.findById(replay.getLiveSessionId())
                .map(LiveClass::getTeacherId)
                .map(teacherId -> teacherRepository.findById(teacherId)
                        .map(Teacher::getUserId)
                        .filter(Objects::nonNull)
                        .filter(userId::equals)
                        .isPresent())
                .orElse(false);
    }

    private static String asText(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Integer asInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.intValue();
        try {
            return Integer.valueOf(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
