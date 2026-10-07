package tz.elmkusoma.oversight.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.oversight.dto.AnnouncementRequest;
import tz.elmkusoma.oversight.dto.AnnouncementResponse;
import tz.elmkusoma.oversight.service.OversightAnnouncementService;
import tz.elmkusoma.oversight.service.OversightScopeResolver;
import tz.elmkusoma.oversight.service.OversightScopeResolver.Scope;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Nationaladmin.md §23 — jurisdictional announcements for governance roles.
 * NATIONWIDE broadcasts are authored by NATIONAL_ADMIN only; REGIONAL_ADMIN
 * authors publish inside their own region — every create goes through
 * {@link OversightScopeResolver} so scopes are never widened by the request
 * body (audienceRegionId/audienceDistrictId are only narrowed into).
 */
@RestController
@RequestMapping("/v1/oversight/announcements")
@RequiredArgsConstructor
public class OversightAnnouncementController {

    private final OversightAnnouncementService announcementService;
    private final OversightScopeResolver scopeResolver;

    @GetMapping
    @PreAuthorize("hasAnyRole('NATIONAL_ADMIN', 'REGIONAL_ADMIN', 'DISTRICT_ADMIN')")
    public ResponseEntity<List<AnnouncementResponse>> list(
            @RequestAttribute("userId") UUID userId,
            @RequestParam(required = false) UUID regionId,
            @RequestParam(required = false) UUID districtId) {
        Scope scope = scopeResolver.resolve(userId, regionId, districtId);
        return ResponseEntity.ok(announcementService.list(scope.regionId(), scope.districtId()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('NATIONAL_ADMIN', 'REGIONAL_ADMIN')")
    public ResponseEntity<AnnouncementResponse> create(
            @RequestAttribute("userId") UUID userId,
            @Valid @RequestBody AnnouncementRequest request) {
        String audience = request.getAudienceType().trim().toUpperCase(Locale.ROOT);
        Scope scope = scopeResolver.resolve(userId, request.getAudienceRegionId(), request.getAudienceDistrictId());
        // The resolver pins regional/district authors to their own jurisdiction;
        // a country-wide broadcast from a regional author would bleed scope.
        if (OversightAnnouncementService.AUDIENCE_NATIONWIDE.equals(audience)
                && (scope.regionId() != null || scope.districtId() != null)) {
            throw new ForbiddenException("nationwide broadcast", "create");
        }
        return ResponseEntity.ok(announcementService.create(request, userId, scope));
    }
}
