package tz.elmkusoma.teacher.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.administration.dto.VerificationSummaryResponse;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.teacher.dto.request.TeacherVerificationSubmitRequest;
import tz.elmkusoma.teacher.dto.response.TeacherVerificationStatusResponse;
import tz.elmkusoma.teacher.service.TeacherVerificationService;

import java.util.UUID;

/**
 * Teacher self-service verification. Provisional teachers may read their state and the
 * document requirements, save draft work elsewhere, and submit — but only a Platform
 * Admin grants formal verification (enforced server-side by the shared publish guard).
 */
@RestController
@RequestMapping("/v1/teachers/me/verification")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER')")
@Tag(name = "Teacher Verification", description = "Self-service teacher verification workflow")
public class TeacherVerificationController {

    private final TeacherVerificationService teacherVerificationService;

    @GetMapping
    @Operation(summary = "Get current teacher's verification state, history, and document requirements")
    public ResponseEntity<ApiResponse<TeacherVerificationStatusResponse>> getMyVerification(
            @RequestHeader(value = "X-Institution-Id", required = false) UUID institutionId,
            @RequestAttribute("userId") UUID userId) {
        return ResponseEntity.ok(
                ApiResponse.success(teacherVerificationService.getMyVerification(userId, institutionId)));
    }

    @PostMapping("/submit")
    @Operation(summary = "Submit a teacher verification application (preliminary document checks run here)")
    public ResponseEntity<ApiResponse<VerificationSummaryResponse>> submit(
            @RequestHeader(value = "X-Institution-Id", required = false) UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @Valid @RequestBody TeacherVerificationSubmitRequest request) {
        VerificationSummaryResponse response =
                teacherVerificationService.submit(userId, institutionId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Verification application submitted", response));
    }
}
