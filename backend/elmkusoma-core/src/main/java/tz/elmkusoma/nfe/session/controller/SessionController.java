package tz.elmkusoma.nfe.session.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.common.PageResponse;
import tz.elmkusoma.nfe.session.dto.SessionRequest;
import tz.elmkusoma.nfe.session.dto.SessionResponse;
import tz.elmkusoma.nfe.session.service.SessionService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/nfe/sessions")
@RequiredArgsConstructor
@Tag(name = "NFE Session Management", description = "CRUD operations for NFE live sessions")
public class SessionController {

    private final SessionService sessionService;

    @PostMapping
    @Operation(summary = "Create a new session")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<SessionResponse>> createSession(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @Valid @RequestBody SessionRequest request) {
        SessionResponse session = sessionService.createSession(institutionId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Session created successfully", session));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a session by ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<SessionResponse>> getSession(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID id) {
        SessionResponse session = sessionService.getSession(institutionId, id);
        return ResponseEntity.ok(ApiResponse.success(session));
    }

    @GetMapping
    @Operation(summary = "List all sessions in an institution")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<PageResponse<SessionResponse>>> listSessions(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<SessionResponse> sessions = sessionService.listSessions(institutionId, page, size);
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }

    @GetMapping("/provider/{providerId}")
    @Operation(summary = "Get sessions by provider ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<SessionResponse>>> getSessionsByProvider(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID providerId) {
        List<SessionResponse> sessions = sessionService.getSessionsByProvider(institutionId, providerId);
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }

    @GetMapping("/program/{programId}")
    @Operation(summary = "Get sessions by program ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<SessionResponse>>> getSessionsByProgram(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID programId) {
        List<SessionResponse> sessions = sessionService.getSessionsByProgram(institutionId, programId);
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get sessions by status")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<SessionResponse>>> getSessionsByStatus(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable String status) {
        List<SessionResponse> sessions = sessionService.getSessionsByStatus(institutionId, status);
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a session")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<SessionResponse>> updateSession(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID id,
            @Valid @RequestBody SessionRequest request) {
        SessionResponse session = sessionService.updateSession(institutionId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Session updated successfully", session));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a session")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSession(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @PathVariable UUID id) {
        sessionService.deleteSession(institutionId, id);
        return ResponseEntity.ok(ApiResponse.success("Session deleted successfully", null));
    }
}
