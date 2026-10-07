package tz.elmkusoma.highereducation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.highereducation.dto.WorkshopSessionDTO;
import tz.elmkusoma.highereducation.service.HighEdIdentity;
import tz.elmkusoma.highereducation.service.WorkshopSessionService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/college/learner/workshops")
@RequiredArgsConstructor
public class WorkshopSessionController {

    private final WorkshopSessionService workshopSessionService;
    private final HighEdIdentity highEdIdentity;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR', 'STUDENT', 'OTHER_LEARNER')")
    public ResponseEntity<ApiResponse<WorkshopSessionDTO>> createSession(
            @RequestBody WorkshopSessionDTO dto,
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute(value = "userRole", required = false) String role,
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId) {
        // §48: server-authoritative scope — the client body never decides the tenant
        if (institutionId == null) {
            throw new ForbiddenException("Access denied");
        }
        dto.setInstitutionId(institutionId);
        if (isLearnerRole(role)) {
            UUID learnerId = highEdIdentity.resolveStudentId(userId, role,
                    dto.getStudentId() != null ? dto.getStudentId() : userId);
            highEdIdentity.assertStudentInInstitution(learnerId, institutionId);
            dto.setStudentId(learnerId);
        }
        return ResponseEntity.ok(ApiResponse.success("Workshop created", workshopSessionService.createSession(dto)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR')")
    public ResponseEntity<ApiResponse<WorkshopSessionDTO>> getSession(
            @PathVariable UUID id,
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId,
            @RequestAttribute(value = "userRole", required = false) String role) {
        WorkshopSessionDTO dto = workshopSessionService.getSession(id);
        if (!canAccessSession(dto, userId, institutionId, role)) {
            // §41/§60: object-level denial — indistinguishable from not-found (no resource oracle)
            throw new ResourceNotFoundException("Workshop session not found");
        }
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<List<WorkshopSessionDTO>>> getStudentSessions(
            @PathVariable UUID studentId,
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId,
            @RequestAttribute(value = "userRole", required = false) String role) {
        UUID learnerId = highEdIdentity.resolveStudentId(userId, role, studentId);
        highEdIdentity.assertStudentInInstitution(learnerId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(
                scopedToInstitution(workshopSessionService.getStudentSessions(learnerId), institutionId, role)));
    }

    @GetMapping("/institution/{institutionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR')")
    public ResponseEntity<ApiResponse<List<WorkshopSessionDTO>>> getInstitutionSessions(
            @PathVariable UUID institutionId,
            @RequestAttribute(value = "institutionId", required = false) UUID callerInstitutionId,
            @RequestAttribute(value = "userRole", required = false) String role) {
        if (!"ADMIN".equals(role)
                && (callerInstitutionId == null || !callerInstitutionId.equals(institutionId))) {
            throw new ForbiddenException("Access denied");
        }
        return ResponseEntity.ok(ApiResponse.success(workshopSessionService.getInstitutionSessions(institutionId)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR')")
    public ResponseEntity<ApiResponse<WorkshopSessionDTO>> updateSession(
            @PathVariable UUID id,
            @RequestBody WorkshopSessionDTO dto,
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId,
            @RequestAttribute(value = "userRole", required = false) String role) {
        WorkshopSessionDTO existing = workshopSessionService.getSession(id);
        assertCanManageSession(existing, institutionId, role);
        return ResponseEntity.ok(ApiResponse.success("Workshop updated", workshopSessionService.updateSession(id, dto)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'STUDENT', 'OTHER_LEARNER')")
    public ResponseEntity<ApiResponse<Void>> deleteSession(
            @PathVariable UUID id,
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId,
            @RequestAttribute(value = "userRole", required = false) String role) {
        WorkshopSessionDTO existing = workshopSessionService.getSession(id);
        if (isLearnerRole(role)) {
            assertLearnerOwnsSession(existing, userId, institutionId, role);
        } else {
            assertCanManageSession(existing, institutionId, role);
        }
        workshopSessionService.deleteSession(id);
        return ResponseEntity.ok(ApiResponse.success("Workshop deleted", null));
    }

    // ── §41/§60/§98 scope helpers ─────────────────────────────────

    /**
     * Fail-closed object-level access: learners own their records (ownership), staff need
     * institution scope; an unscoped platform ADMIN keeps its legacy platform-wide access.
     */
    private boolean canAccessSession(WorkshopSessionDTO dto, UUID userId, UUID institutionId, String role) {
        if (isLearnerRole(role)) {
            return userId != null && userId.equals(dto.getStudentId());
        }
        if (institutionId != null && institutionId.equals(dto.getInstitutionId())) {
            return true;
        }
        return institutionId == null && "ADMIN".equals(role);
    }

    /** Mutations reach here only through @PreAuthorize; staff additionally need institution scope. */
    private void assertCanManageSession(WorkshopSessionDTO dto, UUID institutionId, String role) {
        if (institutionId != null && institutionId.equals(dto.getInstitutionId())) {
            return;
        }
        if (institutionId == null && "ADMIN".equals(role)) {
            return;
        }
        throw new ForbiddenException("Access denied");
    }

    /**
     * Learner deletion is ownership-gated: resolveStudentId refuses any session that
     * belongs to another learner (403), and the tenant is re-asserted afterwards.
     */
    private void assertLearnerOwnsSession(WorkshopSessionDTO dto, UUID userId, UUID institutionId, String role) {
        if (userId == null || dto.getStudentId() == null) {
            throw new ForbiddenException("Access denied");
        }
        UUID learnerId = highEdIdentity.resolveStudentId(userId, role, dto.getStudentId());
        highEdIdentity.assertStudentInInstitution(learnerId, institutionId);
        if (!dto.getStudentId().equals(userId) && !dto.getStudentId().equals(learnerId)) {
            throw new ForbiddenException("Access denied");
        }
    }

    /** §35/§36: results always leave the server scoped — lists never cross tenant boundaries. */
    private List<WorkshopSessionDTO> scopedToInstitution(List<WorkshopSessionDTO> list, UUID institutionId, String role) {
        if (isLearnerRole(role)) {
            // ownership proven above; also drop any record injected from another institution
            return institutionId == null
                    ? list
                    : list.stream().filter(s -> institutionId.equals(s.getInstitutionId())).toList();
        }
        if (institutionId == null) {
            if ("ADMIN".equals(role)) {
                return list; // unscoped platform authority (legacy)
            }
            throw new ForbiddenException("Access denied");
        }
        return list.stream().filter(s -> institutionId.equals(s.getInstitutionId())).toList();
    }

    private boolean isLearnerRole(String role) {
        return "STUDENT".equals(role) || "OTHER_LEARNER".equals(role);
    }
}
