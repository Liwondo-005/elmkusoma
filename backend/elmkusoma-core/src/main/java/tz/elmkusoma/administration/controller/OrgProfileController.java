package tz.elmkusoma.administration.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.administration.dto.*;
import tz.elmkusoma.administration.service.InstitutionAuditService;
import tz.elmkusoma.administration.service.InstitutionScopeService;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.shared.domain.Institution;
import tz.elmkusoma.shared.repository.InstitutionRepository;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/org")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
@Tag(name = "Organization Profile & Audit", description = "Organization profile management and audit logs")
public class OrgProfileController {

    private final InstitutionRepository institutionRepository;
    private final InstitutionScopeService scopeService;
    private final InstitutionAuditService auditService;

    @GetMapping("/profile")
    @Operation(summary = "Get full organization profile with people and activity summaries")
    public ResponseEntity<ApiResponse<OrgProfileResponse>> getProfile(
            @RequestAttribute UUID institutionId,
            @RequestAttribute("userId") UUID userId) {
        scopeService.validateAccess(userId, institutionId, "get_profile");
        Institution inst = institutionRepository.findById(institutionId)
                .orElseThrow(() -> new RuntimeException("Institution not found"));

        List<String> enabledServices = inst.getEnabledServices() != null
                ? List.of(inst.getEnabledServices().split(","))
                : List.of();

        var peopleSummary = OrgProfileResponse.PeopleSummary.builder()
                .totalUsers(scopeService.getUsersInInstitution(institutionId).size())
                .totalTeachers(scopeService.countUsersInInstitution(institutionId,
                        tz.elmkusoma.shared.domain.InstitutionMembership.Role.TEACHER))
                .totalStudents(scopeService.countUsersInInstitution(institutionId,
                        tz.elmkusoma.shared.domain.InstitutionMembership.Role.STUDENT))
                .totalParents(scopeService.countUsersInInstitution(institutionId,
                        tz.elmkusoma.shared.domain.InstitutionMembership.Role.PARENT))
                .activeUsers(scopeService.getUsersInInstitution(institutionId).stream()
                        .filter(u -> Boolean.TRUE.equals(u.getIsActive())).count())
                .pendingInvitations(auditService.countPendingInvitations(institutionId))
                .build();

        var activityItems = auditService.getRecentActivity(institutionId, 10).stream()
                .map(a -> OrgProfileResponse.ActivityItem.builder()
                        .id(a.getId())
                        .actorName(a.getActorName())
                        .activityType(a.getActivityType())
                        .title(a.getTitle())
                        .description(a.getDescription())
                        .createdAt(a.getCreatedAt())
                        .build())
                .toList();

        var activitySummary = OrgProfileResponse.ActivitySummary.builder()
                .unreadNotifications(auditService.countUnreadNotifications(institutionId))
                .recentActivity(activityItems)
                .build();

        OrgProfileResponse response = OrgProfileResponse.builder()
                .id(inst.getId())
                .name(inst.getName())
                .code(inst.getCode())
                .type(inst.getType().name())
                .description(inst.getDescription())
                .address(inst.getAddress())
                .city(inst.getCity())
                .region(inst.getRegion())
                .regionId(inst.getRegionId())
                .districtId(inst.getDistrictId())
                .country(inst.getCountry())
                .phone(inst.getPhone())
                .email(inst.getEmail())
                .website(inst.getWebsite())
                .logoUrl(inst.getLogoUrl())
                .bannerUrl(inst.getBannerUrl())
                .motto(inst.getMotto())
                .foundedYear(inst.getFoundedYear())
                .totalCapacity(inst.getTotalCapacity())
                .isActive(inst.getIsActive())
                .approvedAt(inst.getApprovedAt())
                .createdAt(inst.getCreatedAt())
                .enabledServices(enabledServices)
                .peopleSummary(peopleSummary)
                .activitySummary(activitySummary)
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/profile")
    @Operation(summary = "Update organization profile")
    public ResponseEntity<ApiResponse<OrgProfileResponse>> updateProfile(
            @RequestBody UpdateOrgProfileRequest request,
            @RequestAttribute UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute("userEmail") String userEmail) {
        scopeService.validateAdminAccess(userId, institutionId, "update_profile");
        Institution inst = institutionRepository.findById(institutionId)
                .orElseThrow(() -> new RuntimeException("Institution not found"));

        if (request.getName() != null) inst.setName(request.getName());
        if (request.getDescription() != null) inst.setDescription(request.getDescription());
        if (request.getAddress() != null) inst.setAddress(request.getAddress());
        if (request.getCity() != null) inst.setCity(request.getCity());
        if (request.getRegion() != null) inst.setRegion(request.getRegion());
        if (request.getPhone() != null) inst.setPhone(request.getPhone());
        if (request.getEmail() != null) inst.setEmail(request.getEmail());
        if (request.getWebsite() != null) inst.setWebsite(request.getWebsite());
        if (request.getLogoUrl() != null) inst.setLogoUrl(request.getLogoUrl());
        if (request.getBannerUrl() != null) inst.setBannerUrl(request.getBannerUrl());
        if (request.getMotto() != null) inst.setMotto(request.getMotto());
        if (request.getFoundedYear() != null) inst.setFoundedYear(request.getFoundedYear());
        if (request.getTotalCapacity() != null) inst.setTotalCapacity(request.getTotalCapacity());
        inst.setUpdatedBy(userEmail);

        institutionRepository.save(inst);

        auditService.log(institutionId, userId, userEmail, "UPDATE_PROFILE",
                "INSTITUTION", institutionId.toString(), "Organization profile updated", null);

        return ResponseEntity.ok(ApiResponse.success("Profile updated", null));
    }

    @GetMapping("/audit-log")
    @Operation(summary = "Get institution audit logs")
    public ResponseEntity<ApiResponse<List<InstitutionAuditLogResponse>>> getAuditLog(
            @RequestAttribute UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        scopeService.validateAccess(userId, institutionId, "get_audit_log");
        List<InstitutionAuditLogResponse> response = auditService.getAuditLogs(institutionId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/enabled-services")
    @Operation(summary = "Get enabled services for the institution")
    public ResponseEntity<ApiResponse<List<String>>> getEnabledServices(
            @RequestAttribute UUID institutionId,
            @RequestAttribute("userId") UUID userId) {
        scopeService.validateAccess(userId, institutionId, "get_enabled_services");
        Institution inst = institutionRepository.findById(institutionId)
                .orElseThrow(() -> new RuntimeException("Institution not found"));
        List<String> services = inst.getEnabledServices() != null
                ? List.of(inst.getEnabledServices().split(","))
                : List.of();
        return ResponseEntity.ok(ApiResponse.success(services));
    }

    @PutMapping("/enabled-services")
    @Operation(summary = "Update enabled services for the institution")
    public ResponseEntity<ApiResponse<Void>> updateEnabledServices(
            @RequestBody List<String> services,
            @RequestAttribute UUID institutionId,
            @RequestAttribute("userId") UUID userId,
            @RequestAttribute("userEmail") String userEmail) {
        scopeService.validateAdminAccess(userId, institutionId, "update_enabled_services");
        Institution inst = institutionRepository.findById(institutionId)
                .orElseThrow(() -> new RuntimeException("Institution not found"));
        inst.setEnabledServices(String.join(",", services));
        inst.setUpdatedBy(userEmail);
        institutionRepository.save(inst);

        auditService.log(institutionId, userId, userEmail, "UPDATE_SERVICES",
                "INSTITUTION", institutionId.toString(), "Enabled services updated: " + String.join(", ", services), null);

        return ResponseEntity.ok(ApiResponse.success("Services updated", null));
    }
}
