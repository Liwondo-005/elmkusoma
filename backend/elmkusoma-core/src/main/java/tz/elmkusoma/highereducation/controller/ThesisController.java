package tz.elmkusoma.highereducation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.highereducation.domain.ThesisStatus;
import tz.elmkusoma.highereducation.dto.ThesisDTO;
import tz.elmkusoma.highereducation.service.HighEdIdentity;
import tz.elmkusoma.highereducation.service.ThesisService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/education/theses")
@RequiredArgsConstructor
public class ThesisController {

    private final ThesisService thesisService;
    private final HighEdIdentity highEdIdentity;

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
    public ResponseEntity<ApiResponse<ThesisDTO>> getThesis(
            @PathVariable UUID id,
            @RequestAttribute("userId") UUID callerUserId,
            @RequestAttribute(value = "userRole", required = false) String userRole,
            @RequestAttribute(value = "institutionId", required = false) UUID serverInstitutionId) {
        ThesisDTO thesis = thesisService.getThesis(id);
        assertThesisReadable(thesis, callerUserId, userRole, serverInstitutionId, id);
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
            @Valid @RequestBody ThesisDTO dto,
            @RequestAttribute("institutionId") UUID serverInstitutionId,
            @RequestAttribute("userRole") String userRole) {
        assertThesisTenant(thesisService.getThesis(id), serverInstitutionId, userRole);
        ThesisDTO updated = thesisService.updateThesis(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Thesis updated", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteThesis(
            @PathVariable UUID id,
            @RequestAttribute("institutionId") UUID serverInstitutionId,
            @RequestAttribute("userRole") String userRole) {
        assertThesisTenant(thesisService.getThesis(id), serverInstitutionId, userRole);
        thesisService.deleteThesis(id);
        return ResponseEntity.ok(ApiResponse.success("Thesis deleted", null));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<List<ThesisDTO>>> getStudentTheses(
            @PathVariable UUID studentId,
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @RequestAttribute("userId") UUID callerUserId,
            @RequestAttribute("userRole") String userRole,
            @RequestAttribute("institutionId") UUID serverInstitutionId) {
        UUID learnerId = highEdIdentity.resolveStudentId(callerUserId, userRole, studentId);
        highEdIdentity.assertStudentInInstitution(learnerId, serverInstitutionId);
        List<ThesisDTO> theses = thesisService.getStudentTheses(learnerId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(theses));
    }

    @GetMapping("/supervisor/{supervisorId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR')")
    public ResponseEntity<ApiResponse<List<ThesisDTO>>> getSupervisorTheses(
            @PathVariable UUID supervisorId) {
        List<ThesisDTO> theses = thesisService.getThesesBySupervisor(supervisorId);
        return ResponseEntity.ok(ApiResponse.success(theses));
    }

    private void assertThesisReadable(ThesisDTO thesis, UUID callerUserId, String userRole,
                                      UUID serverInstitutionId, UUID id) {
        if (HighEdIdentity.isLearner(userRole)) {
            if (thesis.getStudentId() != null) {
                try {
                    highEdIdentity.resolveStudentId(callerUserId, userRole, thesis.getStudentId());
                } catch (ForbiddenException e) {
                    throw new ResourceNotFoundException("Thesis", "id", id);
                }
            } else if (serverInstitutionId == null || !serverInstitutionId.equals(thesis.getInstitutionId())) {
                throw new ResourceNotFoundException("Thesis", "id", id);
            }
            return;
        }
        assertThesisTenant(thesis, serverInstitutionId, userRole);
    }

    private void assertThesisTenant(ThesisDTO thesis, UUID serverInstitutionId, String userRole) {
        if ("ADMIN".equals(userRole)) {
            return;
        }
        if (serverInstitutionId == null || !serverInstitutionId.equals(thesis.getInstitutionId())) {
            throw new ForbiddenException("Thesis", "access");
        }
    }
}
