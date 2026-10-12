package tz.elmkusoma.nfe.provider.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tz.elmkusoma.administration.dto.InvitationResponse;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.nfe.provider.dto.ProviderStaffInviteRequest;
import tz.elmkusoma.nfe.provider.dto.ProviderStaffResponsibilityRequest;
import tz.elmkusoma.nfe.provider.dto.ProviderStaffResponse;
import tz.elmkusoma.nfe.provider.service.ProviderStaffService;

import java.util.List;
import java.util.UUID;

/**
 * Provider staff management - the authority level C to D workflow.
 *
 * <p>The {@code @PreAuthorize} expressions here are only the coarse gate that keeps learners out.
 * Every meaningful decision - is this caller an owner of <em>this</em> provider, is the
 * organisation writable, is the target actually this provider's staff - is made server-side in
 * {@link ProviderStaffService}, because a caller who reaches this controller can put any provider
 * id in the path.</p>
 *
 * <p>Note that no {@code institutionId} request attribute is consulted. Provider scope is resolved
 * from the provider row itself, so a client cannot steer a call at another tenant by sending a
 * different {@code X-Institution-Id}.</p>
 */
@RestController
@RequestMapping("/v1/nfe/providers/{providerId}/staff")
@RequiredArgsConstructor
@Tag(name = "NFE Provider Staff", description = "Provider staff provisioning and lifecycle")
public class ProviderStaffController {

    private final ProviderStaffService providerStaffService;

    @GetMapping
    @Operation(summary = "List this provider's staff")
    @PreAuthorize("hasAnyRole('ADMIN', 'NATIONAL_ADMIN', 'PROVIDER_ADMIN')")
    public ResponseEntity<ApiResponse<List<ProviderStaffResponse>>> listStaff(
            @RequestAttribute("userId") UUID actorId,
            @RequestAttribute("userRole") String actorRole,
            @PathVariable UUID providerId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Provider staff retrieved successfully", providerStaffService.listStaff(actorId, actorRole, providerId)));
    }

    @PostMapping("/invitations")
    @Operation(summary = "Invite a staff member to this provider")
    @PreAuthorize("hasAnyRole('ADMIN', 'NATIONAL_ADMIN', 'PROVIDER_ADMIN')")
    public ResponseEntity<ApiResponse<InvitationResponse>> inviteStaff(
            @RequestAttribute("userId") UUID actorId,
            @RequestAttribute("userRole") String actorRole,
            @PathVariable UUID providerId,
            @Valid @RequestBody ProviderStaffInviteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Staff invitation issued successfully",
                        providerStaffService.inviteStaff(actorId, actorRole, providerId, request)));
    }

    @PutMapping("/{staffUserId}/responsibility")
    @Operation(summary = "Assign or clear a staff member's responsibility")
    @PreAuthorize("hasAnyRole('ADMIN', 'NATIONAL_ADMIN', 'PROVIDER_ADMIN')")
    public ResponseEntity<ApiResponse<ProviderStaffResponse>> setResponsibility(
            @RequestAttribute("userId") UUID actorId,
            @RequestAttribute("userRole") String actorRole,
            @PathVariable UUID providerId,
            @PathVariable UUID staffUserId,
            @Valid @RequestBody ProviderStaffResponsibilityRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Responsibility updated successfully",
                providerStaffService.setResponsibility(actorId, actorRole, providerId, staffUserId,
                        request.getResponsibility())));
    }

    @DeleteMapping("/{staffUserId}")
    @Operation(summary = "Remove a staff member from this provider (their account is kept)")
    @PreAuthorize("hasAnyRole('ADMIN', 'NATIONAL_ADMIN', 'PROVIDER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> removeStaff(
            @RequestAttribute("userId") UUID actorId,
            @RequestAttribute("userRole") String actorRole,
            @PathVariable UUID providerId,
            @PathVariable UUID staffUserId) {
        providerStaffService.removeStaff(actorId, actorRole, providerId, staffUserId);
        return ResponseEntity.ok(ApiResponse.success("Staff member removed from this provider", null));
    }
}
