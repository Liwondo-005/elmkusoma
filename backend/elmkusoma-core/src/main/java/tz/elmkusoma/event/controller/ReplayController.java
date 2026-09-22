package tz.elmkusoma.event.controller;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.event.domain.Replay;
import tz.elmkusoma.event.repository.ReplayRepository;

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

    @GetMapping
    public ResponseEntity<ApiResponse<List<Replay>>> getAllReplays() {
        List<Replay> replays = replayRepository.findByStatusAndIsDeletedFalse("AVAILABLE");
        log.info("Replays listed: count={}", replays.size());
        return ResponseEntity.ok(ApiResponse.success("Replays retrieved", replays));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Replay>> getReplay(@PathVariable UUID id) {
        return replayRepository.findById(id)
                .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                .map(r -> {
                    log.info("Replay accessed: id={}, eventId={}, status={}", r.getId(), r.getEventId(), r.getStatus());
                    return ResponseEntity.ok(ApiResponse.success("Replay found", r));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<ApiResponse<List<Replay>>> getReplaysByEvent(@PathVariable UUID eventId) {
        List<Replay> replays = replayRepository.findByEventIdAndIsDeletedFalse(eventId);
        log.info("Replays by event: eventId={}, count={}", eventId, replays.size());
        return ResponseEntity.ok(ApiResponse.success("Replays retrieved", replays));
    }

    @PutMapping("/{id}/progress")
    public ResponseEntity<ApiResponse<Void>> updateProgress(
            @PathVariable UUID id,
            @RequestBody Map<String, Integer> body) {
        return replayRepository.findById(id)
                .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                .map(replay -> {
                    int newPosition = body.getOrDefault("position", 0);
                    replay.setLastPositionSeconds(newPosition);
                    replayRepository.save(replay);
                    log.info("Replay progress updated: id={}, position={}s", id, newPosition);
                    return ResponseEntity.ok(ApiResponse.<Void>success("Progress updated", null));
                })
                .orElse(ResponseEntity.notFound().<ApiResponse<Void>>build());
    }
}
