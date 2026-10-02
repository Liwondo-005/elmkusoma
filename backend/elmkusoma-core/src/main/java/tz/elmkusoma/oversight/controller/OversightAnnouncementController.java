package tz.elmkusoma.oversight.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.oversight.dto.AnnouncementRequest;
import tz.elmkusoma.oversight.dto.AnnouncementResponse;
import tz.elmkusoma.oversight.service.OversightAnnouncementService;
import tz.elmkusoma.oversight.service.OversightScopeResolver;
import tz.elmkusoma.oversight.service.OversightScopeResolver.Scope;

import java.util.List;
import java.util.UUID;

/**
 * Nationaladmin.md §23 — jurisdictional announcements for governance roles.
 * Nationwide/region/district broadcasts are authored by NATIONAL_ADMIN only;
 * every reader goes through {@link OversightScopeResolver} so scopes are never
 * widened by query parameters.
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
    @PreAuthorize("hasRole('NATIONAL_ADMIN')")
    public ResponseEntity<AnnouncementResponse> create(
            @RequestAttribute("userId") UUID userId,
            @Valid @RequestBody AnnouncementRequest request) {
        return ResponseEntity.ok(announcementService.create(request, userId));
    }
}
