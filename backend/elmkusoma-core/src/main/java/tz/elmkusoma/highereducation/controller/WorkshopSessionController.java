package tz.elmkusoma.highereducation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.highereducation.dto.WorkshopSessionDTO;
import tz.elmkusoma.highereducation.service.WorkshopSessionService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/college/learner/workshops")
@RequiredArgsConstructor
public class WorkshopSessionController {

    private final WorkshopSessionService workshopSessionService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR')")
    public ResponseEntity<ApiResponse<WorkshopSessionDTO>> createSession(
            @RequestBody WorkshopSessionDTO dto) {
        return ResponseEntity.ok(ApiResponse.success("Workshop created", workshopSessionService.createSession(dto)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR')")
    public ResponseEntity<ApiResponse<WorkshopSessionDTO>> getSession(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(workshopSessionService.getSession(id)));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<List<WorkshopSessionDTO>>> getStudentSessions(
            @PathVariable UUID studentId) {
        return ResponseEntity.ok(ApiResponse.success(workshopSessionService.getStudentSessions(studentId)));
    }

    @GetMapping("/institution/{institutionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR')")
    public ResponseEntity<ApiResponse<List<WorkshopSessionDTO>>> getInstitutionSessions(
            @PathVariable UUID institutionId) {
        return ResponseEntity.ok(ApiResponse.success(workshopSessionService.getInstitutionSessions(institutionId)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR')")
    public ResponseEntity<ApiResponse<WorkshopSessionDTO>> updateSession(
            @PathVariable UUID id, @RequestBody WorkshopSessionDTO dto) {
        return ResponseEntity.ok(ApiResponse.success("Workshop updated", workshopSessionService.updateSession(id, dto)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSession(@PathVariable UUID id) {
        workshopSessionService.deleteSession(id);
        return ResponseEntity.ok(ApiResponse.success("Workshop deleted", null));
    }
}
