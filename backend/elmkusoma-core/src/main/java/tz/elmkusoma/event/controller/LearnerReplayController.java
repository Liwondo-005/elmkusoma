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
@RequestMapping("/v1/learner/replays")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN')")
public class LearnerReplayController {

    private static final Logger log = LoggerFactory.getLogger(LearnerReplayController.class);

    private final ReplayRepository replayRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Replay>>> getReplays() {
        List<Replay> replays = replayRepository.findByStatusAndIsDeletedFalse("AVAILABLE");
        return ResponseEntity.ok(ApiResponse.success(replays));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Replay>> getReplay(@PathVariable UUID id) {
        return replayRepository.findById(id)
                .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                .map(r -> {
                    r.setViewCount(r.getViewCount() + 1);
                    replayRepository.save(r);
                    return ResponseEntity.ok(ApiResponse.success(r));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/progress")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReplayProgress(@PathVariable UUID id) {
        return replayRepository.findById(id)
                .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                .map(r -> {
                    Map<String, Object> progress = Map.of(
                            "position", r.getLastPositionSeconds(),
                            "viewCount", r.getViewCount(),
                            "completed", r.getLastPositionSeconds() > 0 && r.getDurationSeconds() != null && r.getLastPositionSeconds() >= r.getDurationSeconds() - 10
                    );
                    return ResponseEntity.ok(ApiResponse.success(progress));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/progress")
    public ResponseEntity<ApiResponse<Void>> updateReplayProgress(
            @PathVariable UUID id,
            @RequestBody Map<String, Integer> body,
            @RequestAttribute("userId") UUID userId) {
        return replayRepository.findById(id)
                .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                .map(replay -> {
                    int newPosition = body.getOrDefault("position", 0);
                    replay.setLastPositionSeconds(newPosition);
                    replayRepository.save(replay);
                    return ResponseEntity.ok(ApiResponse.<Void>success(null));
                })
                .orElse(ResponseEntity.notFound().<ApiResponse<Void>>build());
    }
}
