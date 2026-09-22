package tz.elmkusoma.event.controller;

import lombok.RequiredArgsConstructor;
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

    private final ReplayRepository replayRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Replay>>> getAllReplays() {
        return ResponseEntity.ok(ApiResponse.success("Replays retrieved", replayRepository.findByStatusAndIsDeletedFalse("AVAILABLE")));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Replay>> getReplay(@PathVariable UUID id) {
        return replayRepository.findById(id)
                .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                .map(r -> ResponseEntity.ok(ApiResponse.success("Replay found", r)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<ApiResponse<List<Replay>>> getReplaysByEvent(@PathVariable UUID eventId) {
        return ResponseEntity.ok(ApiResponse.success("Replays retrieved", replayRepository.findByEventIdAndIsDeletedFalse(eventId)));
    }

    @PutMapping("/{id}/progress")
    public ResponseEntity<ApiResponse<Void>> updateProgress(
            @PathVariable UUID id,
            @RequestBody Map<String, Integer> body,
            @RequestAttribute("userId") UUID userId) {
        return replayRepository.findById(id)
                .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                .map(replay -> {
                    replay.setLastPositionSeconds(body.getOrDefault("position", 0));
                    replayRepository.save(replay);
                    return ResponseEntity.ok(ApiResponse.<Void>success("Progress updated", null));
                })
                .orElse(ResponseEntity.notFound().<ApiResponse<Void>>build());
    }
}
