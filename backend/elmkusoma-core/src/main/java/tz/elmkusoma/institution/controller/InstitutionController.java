package tz.elmkusoma.institution.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.common.PageResponse;
import tz.elmkusoma.institution.dto.request.CreateInstitutionRequest;
import tz.elmkusoma.institution.dto.request.UpdateInstitutionRequest;
import tz.elmkusoma.institution.dto.response.InstitutionResponse;
import tz.elmkusoma.institution.service.InstitutionService;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;

import java.util.UUID;

@RestController
@RequestMapping("/v1/institutions")
@PreAuthorize("hasAnyRole('INSTITUTION_ADMIN','ADMIN')")
public class InstitutionController {

    private final InstitutionService institutionService;
    private final UserRepository userRepository;

    public InstitutionController(InstitutionService institutionService, UserRepository userRepository) {
        this.institutionService = institutionService;
        this.userRepository = userRepository;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<InstitutionResponse>> createInstitution(
            @Valid @RequestBody CreateInstitutionRequest request) {
        UUID ownerUserId = getCurrentUserId();
        InstitutionResponse response = institutionService.createInstitution(request, ownerUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Institution created successfully", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<InstitutionResponse>> getInstitution(
            @PathVariable UUID id,
            @RequestAttribute("institutionId") UUID ctxInstitutionId,
            @RequestAttribute("userRole") String userRole) {
        assertWithinScope(id, ctxInstitutionId, userRole);
        InstitutionResponse response = institutionService.getInstitution(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<InstitutionResponse>>> listInstitutions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<InstitutionResponse> response = institutionService.listInstitutions(page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<InstitutionResponse>> updateInstitution(
            @PathVariable UUID id,
            @RequestAttribute(value = "userId", required = false) String actorUserId,
            @Valid @RequestBody UpdateInstitutionRequest request,
            @RequestAttribute("institutionId") UUID ctxInstitutionId,
            @RequestAttribute("userRole") String userRole) {
        assertWithinScope(id, ctxInstitutionId, userRole);
        InstitutionResponse response = institutionService.updateInstitution(id, request, actorId(actorUserId));
        return ResponseEntity.ok(ApiResponse.success("Institution updated successfully", response));
    }

    /**
     * Platform admins may touch any institution; an INSTITUTION_ADMIN may only
     * touch the institution resolved server-side from their own membership.
     */
    private void assertWithinScope(UUID targetId, UUID ctxInstitutionId, String userRole) {
        boolean platformAdmin = "ADMIN".equals(userRole) || "NATIONAL_ADMIN".equals(userRole);
        if (!platformAdmin && (ctxInstitutionId == null || !targetId.equals(ctxInstitutionId))) {
            throw new SecurityException("You are not authorized to access this institution");
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteInstitution(
            @PathVariable UUID id,
            @RequestAttribute(value = "userId", required = false) String actorUserId) {
        institutionService.deleteInstitution(id, actorId(actorUserId));
        return ResponseEntity.ok(ApiResponse.success("Institution deleted successfully", null));
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<InstitutionResponse>> activateInstitution(@PathVariable UUID id) {
        InstitutionResponse response = institutionService.activateInstitution(id);
        return ResponseEntity.ok(ApiResponse.success("Institution activated", response));
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<InstitutionResponse>> deactivateInstitution(@PathVariable UUID id) {
        InstitutionResponse response = institutionService.deactivateInstitution(id);
        return ResponseEntity.ok(ApiResponse.success("Institution deactivated", response));
    }

    /** Parses the actor id published by the JWT request-attribute filter; null when absent. */
    private UUID actorId(String rawUserId) {
        if (rawUserId == null || rawUserId.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(rawUserId.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
        return user.getId();
    }
}
