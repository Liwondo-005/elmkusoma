package tz.elmkusoma.administration.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.administration.dto.*;
import tz.elmkusoma.administration.service.InstitutionAuditService;
import tz.elmkusoma.administration.service.InstitutionPeopleService;
import tz.elmkusoma.administration.service.InstitutionScopeService;
import tz.elmkusoma.common.ApiResponse;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/people")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
@Tag(name = "Institution People Management", description = "Manage users within an institution")
public class InstitutionPeopleController {

    private final InstitutionPeopleService peopleService;
    private final InstitutionScopeService scopeService;
    private final InstitutionAuditService auditService;

    @GetMapping
    @Operation(summary = "List all users in institution with membership info")
    public ResponseEntity<ApiResponse<List<PeopleMemberResponse>>> listPeople(
            @RequestAttribute UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        scopeService.validateAccess(userId, institutionId, "list_people");
        List<PeopleMemberResponse> response = peopleService.listPeople(institutionId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{memberUserId}")
    @Operation(summary = "Get a specific member's details")
    public ResponseEntity<ApiResponse<PeopleMemberResponse>> getMember(
            @PathVariable UUID memberUserId,
            @RequestAttribute UUID institutionId,
            @RequestAttribute("userId") UUID userId) {
        scopeService.validateAccess(userId, institutionId, "get_member");
        PeopleMemberResponse response = peopleService.getMember(institutionId, memberUserId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{memberUserId}/role")
    @Operation(summary = "Update a member's role within the institution")
    public ResponseEntity<ApiResponse<PeopleMemberResponse>> updateMemberRole(
            @PathVariable UUID memberUserId,
            @RequestParam String newRole,
            @RequestAttribute UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute("userEmail") String userEmail) {
        scopeService.validateAdminAccess(userId, institutionId, "update_member_role");
        PeopleMemberResponse response = peopleService.updateMemberRole(institutionId, memberUserId, newRole, userEmail);
        auditService.log(institutionId, userId, userEmail, "UPDATE_ROLE",
                "USER", memberUserId.toString(), "Role changed to " + newRole, null);
        return ResponseEntity.ok(ApiResponse.success("Role updated", response));
    }

    @PutMapping("/{memberUserId}/deactivate")
    @Operation(summary = "Deactivate a member")
    public ResponseEntity<ApiResponse<Void>> deactivateMember(
            @PathVariable UUID memberUserId,
            @RequestAttribute UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute("userEmail") String userEmail) {
        scopeService.validateAdminAccess(userId, institutionId, "deactivate_member");
        peopleService.deactivateMember(institutionId, memberUserId);
        auditService.log(institutionId, userId, userEmail, "DEACTIVATE_USER",
                "USER", memberUserId.toString(), "Member deactivated", null);
        return ResponseEntity.ok(ApiResponse.success("Member deactivated", null));
    }

    @PutMapping("/{memberUserId}/activate")
    @Operation(summary = "Activate a member")
    public ResponseEntity<ApiResponse<Void>> activateMember(
            @PathVariable UUID memberUserId,
            @RequestAttribute UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute("userEmail") String userEmail) {
        scopeService.validateAdminAccess(userId, institutionId, "activate_member");
        peopleService.activateMember(institutionId, memberUserId);
        auditService.log(institutionId, userId, userEmail, "ACTIVATE_USER",
                "USER", memberUserId.toString(), "Member activated", null);
        return ResponseEntity.ok(ApiResponse.success("Member activated", null));
    }

    @PostMapping("/invite")
    @Operation(summary = "Invite a user to the institution")
    public ResponseEntity<ApiResponse<InvitationResponse>> inviteUser(
            @Valid @RequestBody InviteUserRequest request,
            @RequestAttribute UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute("userEmail") String userEmail) {
        scopeService.validateAdminAccess(userId, institutionId, "invite_user");
        InvitationResponse response = peopleService.inviteUser(institutionId, request, userId);
        auditService.log(institutionId, userId, userEmail, "INVITE_USER",
                "INVITATION", response.getId().toString(), "Invitation sent to " + request.getEmail(), null);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Invitation sent", response));
    }

    @GetMapping("/invitations")
    @Operation(summary = "List all pending invitations")
    public ResponseEntity<ApiResponse<List<InvitationResponse>>> listInvitations(
            @RequestAttribute UUID institutionId,
            @RequestAttribute("userId") UUID userId) {
        scopeService.validateAccess(userId, institutionId, "list_invitations");
        List<InvitationResponse> response = peopleService.listInvitations(institutionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/invitations/{invitationId}")
    @Operation(summary = "Cancel a pending invitation")
    public ResponseEntity<ApiResponse<Void>> cancelInvitation(
            @PathVariable UUID invitationId,
            @RequestAttribute UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute("userEmail") String userEmail) {
        scopeService.validateAdminAccess(userId, institutionId, "cancel_invitation");
        peopleService.cancelInvitation(institutionId, invitationId);
        auditService.log(institutionId, userId, userEmail, "CANCEL_INVITATION",
                "INVITATION", invitationId.toString(), "Invitation cancelled", null);
        return ResponseEntity.ok(ApiResponse.success("Invitation cancelled", null));
    }
}
