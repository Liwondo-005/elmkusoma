package tz.elmkusoma.shared.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.shared.dto.PublicLiveClassView;
import tz.elmkusoma.shared.service.PublicLiveClassService;

import java.util.List;

/**
 * Anonymous live-class discovery.
 *
 * <p>Sits under the already-permitted {@code /v1/public/**} prefix. There is deliberately no
 * status filter, no institution filter and no "include restricted" parameter: eligibility is
 * decided by the repository query, so there is no input a caller could supply to widen it.</p>
 *
 * <p>Joining is not here. The authenticated join endpoint
 * ({@code POST /v1/live-session/join/{classId}}) is untouched and revalidates role, membership,
 * class-group enrolment and live status server-side. A public card linking to a class does not
 * grant access to it.</p>
 */
@RestController
@RequestMapping("/v1/public/live-classes")
@RequiredArgsConstructor
@Tag(name = "Public Site", description = "Published live classes and learning events")
public class PublicLiveClassController {

    private final PublicLiveClassService publicLiveClassService;

    @GetMapping("/live")
    @Operation(summary = "Live now: published, public classes genuinely in progress")
    public ResponseEntity<ApiResponse<List<PublicLiveClassView>>> liveNow(
            @RequestParam(defaultValue = "6") int limit) {
        return ResponseEntity.ok(ApiResponse.success(publicLiveClassService.liveNow(limit)));
    }

    @GetMapping("/upcoming")
    @Operation(summary = "Upcoming: published, public classes scheduled to start in the future")
    public ResponseEntity<ApiResponse<List<PublicLiveClassView>>> upcoming(
            @RequestParam(defaultValue = "6") int limit) {
        return ResponseEntity.ok(ApiResponse.success(publicLiveClassService.upcoming(limit)));
    }

    @GetMapping("/past")
    @Operation(summary = "Past classes that finished, with replay availability only")
    public ResponseEntity<ApiResponse<List<PublicLiveClassView>>> past(
            @RequestParam(defaultValue = "6") int limit) {
        return ResponseEntity.ok(ApiResponse.success(publicLiveClassService.past(limit)));
    }
}