package tz.elmkusoma.highereducation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.highereducation.domain.ThesisStatus;
import tz.elmkusoma.highereducation.dto.ThesisDTO;
import tz.elmkusoma.highereducation.service.ThesisService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/education/theses")
@RequiredArgsConstructor
public class ThesisController {

    private final ThesisService thesisService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR', 'STUDENT', 'OTHER_LEARNER')")
    public ResponseEntity<ApiResponse<List<ThesisDTO>>> listTheses(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestParam(required = false) ThesisStatus status) {
        List<ThesisDTO> theses = status != null
                ? thesisService.getThesesByStatus(institutionId, status)
                : thesisService.getTheses(institutionId);
        return ResponseEntity.ok(ApiResponse.success(theses));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR', 'STUDENT', 'OTHER_LEARNER')")
    public ResponseEntity<ApiResponse<ThesisDTO>> getThesis(@PathVariable UUID id) {
        ThesisDTO thesis = thesisService.getThesis(id);
        return ResponseEntity.ok(ApiResponse.success(thesis));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<ThesisDTO>> createThesis(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @Valid @RequestBody ThesisDTO dto) {
        dto.setInstitutionId(institutionId);
        ThesisDTO created = thesisService.createThesis(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Thesis created", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<ThesisDTO>> updateThesis(
            @PathVariable UUID id,
            @Valid @RequestBody ThesisDTO dto) {
        ThesisDTO updated = thesisService.updateThesis(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Thesis updated", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteThesis(@PathVariable UUID id) {
        thesisService.deleteThesis(id);
        return ResponseEntity.ok(ApiResponse.success("Thesis deleted", null));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<List<ThesisDTO>>> getStudentTheses(
            @PathVariable UUID studentId,
            @RequestHeader("X-Institution-Id") UUID institutionId) {
        List<ThesisDTO> theses = thesisService.getStudentTheses(studentId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(theses));
    }

    @GetMapping("/supervisor/{supervisorId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR')")
    public ResponseEntity<ApiResponse<List<ThesisDTO>>> getSupervisorTheses(
            @PathVariable UUID supervisorId) {
        List<ThesisDTO> theses = thesisService.getThesesBySupervisor(supervisorId);
        return ResponseEntity.ok(ApiResponse.success(theses));
    }
}
