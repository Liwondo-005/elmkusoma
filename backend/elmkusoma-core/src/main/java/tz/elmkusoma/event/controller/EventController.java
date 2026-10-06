package tz.elmkusoma.event.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.event.dto.*;
import tz.elmkusoma.event.service.EventService;
import tz.elmkusoma.exception.ForbiddenException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/events")
@PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'PROVIDER_ADMIN', 'PROVIDER_STAFF')")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<EventResponse>>> getEvents(
            HttpServletRequest request,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String providerId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        UUID institutionId = getInstitutionId(request);
        String effectiveProviderId = scopedProviderId(providerId, request);
        String role = getRequestRole(request);
        if (institutionId == null && !isPlatformRole(role) && effectiveProviderId == null) {
            // §48: findFiltered treats a null institution as "all tenants" — an unscoped
            // non-platform caller must never receive an unfiltered list.
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
        }

        // §81/§82: optional Pageable pagination; absent page/size keeps legacy full-list behavior
        if (page != null || size != null) {
            Page<EventResponse> result = eventService.getEvents(institutionId, status, eventType, category,
                    effectiveProviderId, pageRequest(page, size));
            return pagedResponse(result);
        }
        List<EventResponse> events = eventService.getEvents(institutionId, status, eventType, category,
                effectiveProviderId);
        return ResponseEntity.ok(ApiResponse.success(events));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getEvent(
            @PathVariable UUID id,
            HttpServletRequest request) {
        EventResponse event = eventService.getEventByIdForAdmin(id);
        if (!canAccessEvent(event, request)) {
            // §41/§60/§98: object-level (ID-manipulation) denial across institutions
            return ResponseEntity.status(404).body(ApiResponse.error("Event not found"));
        }
        return ResponseEntity.ok(ApiResponse.success(event));
    }

    @GetMapping("/institution/{institutionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'STUDENT', 'OTHER_LEARNER')")
    public ResponseEntity<ApiResponse<List<EventResponse>>> getEventsByInstitution(
            @PathVariable UUID institutionId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String providerId,
            HttpServletRequest request) {
        UUID callerInstitutionId = getInstitutionId(request);
        String role = getRequestRole(request);
        if (!isPlatformRole(role)
                && (callerInstitutionId == null || !callerInstitutionId.equals(institutionId))) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
        }
        String effectiveStatus = status;
        if ((effectiveStatus == null || effectiveStatus.isBlank()) && isLearnerRole(role)) {
            effectiveStatus = "PUBLISHED";
        }

        // §82: real Pageable slicing replaces the previous in-memory subList(0, limit)
        if (page != null || size != null || limit != null) {
            int pageSize = size != null ? size : (limit != null ? limit : 20);
            Page<EventResponse> result = eventService.getEvents(institutionId, effectiveStatus, null, null,
                    providerId, pageRequest(page, pageSize));
            return pagedResponse(result);
        }
        List<EventResponse> events = eventService.getEvents(institutionId, effectiveStatus, null, null, providerId);
        return ResponseEntity.ok(ApiResponse.success(events));
    }

    @GetMapping("/registrations/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'STUDENT', 'OTHER_LEARNER')")
    public ResponseEntity<ApiResponse<List<EventRegistrationResponse>>> getRegistrationsForUser(
            @PathVariable UUID userId,
            HttpServletRequest request) {
        UUID callerId = getUserId(request);
        String role = getRequestRole(request);
        if ((callerId == null || !callerId.equals(userId)) && !isAdminRole(role)) {
            throw new ForbiddenException("You can only view your own registrations");
        }
        List<EventRegistrationResponse> registrations = eventService.getUserRegistrations(userId);
        return ResponseEntity.ok(ApiResponse.success(registrations));
    }

    @GetMapping("/registrations/event/{eventId}")
    public ResponseEntity<ApiResponse<List<EventRegistrationResponse>>> getRegistrationsForEvent(
            @PathVariable UUID eventId,
            HttpServletRequest request) {
        UUID userId = getUserId(request);
        EventResponse existing = eventService.getEventByIdForAdmin(eventId);
        if (!canAccessEvent(existing, request)) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
        }
        List<EventRegistrationResponse> registrations = eventService.getEventRegistrations(eventId, userId);
        return ResponseEntity.ok(ApiResponse.success(registrations));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EventResponse>> createEvent(
            @Valid @RequestBody EventRequest request,
            HttpServletRequest httpRequest) {
        UUID institutionId = getInstitutionId(httpRequest);
        UUID userId = getUserId(httpRequest);
        // §48: provider_id is server-authoritative on create — never trusted from the client
        request.setProviderId(resolveCreatedProviderId(request.getProviderId(), httpRequest));
        EventResponse event = eventService.createEvent(institutionId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Event created", event));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EventResponse>> updateEvent(
            @PathVariable UUID id,
            @Valid @RequestBody EventRequest request,
            HttpServletRequest httpRequest) {
        EventResponse existing = eventService.getEventByIdForAdmin(id);
        if (!canAccessEvent(existing, httpRequest)) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
        }
        assertProviderCanManage(existing, httpRequest);
        EventResponse event = eventService.updateEvent(id, request);
        return ResponseEntity.ok(ApiResponse.success("Event updated", event));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteEvent(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "false") boolean force,
            HttpServletRequest httpRequest) {
        EventResponse existing = eventService.getEventByIdForAdmin(id);
        if (!canAccessEvent(existing, httpRequest)) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
        }
        assertProviderCanManage(existing, httpRequest);
        eventService.deleteEvent(id, force);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/{id}/registrations")
    public ResponseEntity<ApiResponse<List<EventRegistrationResponse>>> getEventRegistrations(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {
        UUID userId = getUserId(httpRequest);
        EventResponse existing = eventService.getEventByIdForAdmin(id);
        if (!canAccessEvent(existing, httpRequest)) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
        }
        List<EventRegistrationResponse> registrations = eventService.getEventRegistrations(id, userId);
        return ResponseEntity.ok(ApiResponse.success(registrations));
    }

    @GetMapping("/{id}/materials")
    public ResponseEntity<ApiResponse<List<EventMaterialResponse>>> getEventMaterials(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {
        EventResponse existing = eventService.getEventByIdForAdmin(id);
        if (!canAccessEvent(existing, httpRequest)) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
        }
        List<EventMaterialResponse> materials = eventService.getEventMaterials(id);
        return ResponseEntity.ok(ApiResponse.success(materials));
    }

    @PostMapping("/{id}/materials")
    public ResponseEntity<ApiResponse<EventMaterialResponse>> addEventMaterial(
            @PathVariable UUID id,
            @Valid @RequestBody EventMaterialRequest request,
            HttpServletRequest httpRequest) {
        UUID userId = getUserId(httpRequest);
        EventResponse existing = eventService.getEventByIdForAdmin(id);
        if (!canAccessEvent(existing, httpRequest)) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
        }
        request.setEventId(id);
        EventMaterialResponse material = eventService.addEventMaterial(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Material added", material));
    }

    @DeleteMapping("/materials/{materialId}")
    public ResponseEntity<ApiResponse<Void>> deleteEventMaterial(
            @PathVariable UUID materialId,
            HttpServletRequest httpRequest) {
        // §41/§98: material deletion is object-level authorization — resolve the owning event first
        UUID eventId = eventService.getEventIdForMaterial(materialId);
        EventResponse existing = eventService.getEventByIdForAdmin(eventId);
        if (!canAccessEvent(existing, httpRequest)) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
        }
        eventService.deleteEventMaterial(materialId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<EventResponse>> publishEvent(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {
        UUID institutionId = getInstitutionId(httpRequest);
        EventResponse existing = eventService.getEventByIdForAdmin(id);
        if (!canAccessEvent(existing, httpRequest)) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
        }
        assertProviderCanManage(existing, httpRequest);
        EventResponse event = eventService.publishEvent(id, institutionId);
        return ResponseEntity.ok(ApiResponse.success("Event published", event));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<EventResponse>> cancelEvent(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body,
            HttpServletRequest httpRequest) {
        UUID institutionId = getInstitutionId(httpRequest);
        EventResponse existing = eventService.getEventByIdForAdmin(id);
        if (!canAccessEvent(existing, httpRequest)) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
        }
        assertProviderCanManage(existing, httpRequest);
        String reason = body != null ? body.getOrDefault("reason", "") : "";
        EventResponse event = eventService.cancelEvent(id, institutionId, reason);
        return ResponseEntity.ok(ApiResponse.success("Event cancelled", event));
    }

    @PostMapping("/{id}/start-live")
    public ResponseEntity<ApiResponse<EventResponse>> startLiveEvent(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {
        UUID institutionId = getInstitutionId(httpRequest);
        EventResponse existing = eventService.getEventByIdForAdmin(id);
        if (!canAccessEvent(existing, httpRequest)) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
        }
        assertProviderCanManage(existing, httpRequest);
        EventResponse event = eventService.startLiveEvent(id, institutionId);
        return ResponseEntity.ok(ApiResponse.success("Event started live", event));
    }

    @PostMapping("/{id}/end-live")
    public ResponseEntity<ApiResponse<EventResponse>> endLiveEvent(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {
        UUID institutionId = getInstitutionId(httpRequest);
        EventResponse existing = eventService.getEventByIdForAdmin(id);
        if (!canAccessEvent(existing, httpRequest)) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
        }
        assertProviderCanManage(existing, httpRequest);
        EventResponse event = eventService.endLiveEvent(id, institutionId);
        return ResponseEntity.ok(ApiResponse.success("Event ended", event));
    }

    @GetMapping("/{id}/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getEventSummary(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {
        UUID institutionId = getInstitutionId(httpRequest);
        EventResponse existing = eventService.getEventByIdForAdmin(id);
        if (!canAccessEvent(existing, httpRequest)) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
        }
        Map<String, Object> summary = eventService.getEventSummary(id, institutionId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    // ==================== Helpers ====================

    private PageRequest pageRequest(Integer page, Integer size) {
        int p = page != null && page >= 0 ? page : 0;
        int s = size != null && size > 0 ? size : 20;
        return PageRequest.of(p, s, Sort.by("startsAt").ascending());
    }

    private ResponseEntity<ApiResponse<List<EventResponse>>> pagedResponse(Page<EventResponse> result) {
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(result.getTotalElements()))
                .body(ApiResponse.success(result.getContent()));
    }

    /** §97: provider-role callers are always scoped to their own events. */
    private String scopedProviderId(String providerId, HttpServletRequest request) {
        String role = getRequestRole(request);
        if (isProviderRole(role)) {
            UUID userId = getUserId(request);
            if (userId == null) {
                // §48: never fall back to a client-supplied provider filter — fail closed
                throw new ForbiddenException("Access denied");
            }
            return userId.toString();
        }
        return providerId;
    }

    /**
     * §48: provider_id is server-authoritative on create. Provider-role callers are pinned to
     * their own identity, admin governance (platform or institution-scoped) may label explicitly
     * — every other client-supplied value is dropped because ownership cannot be verified.
     */
    private String resolveCreatedProviderId(String clientProviderId, HttpServletRequest request) {
        String role = getRequestRole(request);
        if (isProviderRole(role)) {
            UUID userId = getUserId(request);
            if (userId == null) {
                throw new ForbiddenException("Access denied");
            }
            return userId.toString();
        }
        if (isAdminRole(role)) {
            return clientProviderId;
        }
        return null;
    }

    /**
     * §41/§60/§97/§98: fail-closed object-level (ID-manipulation) access check for one event.
     * Access requires either institution scope (the caller's resolved institution owns the event)
     * or provider ownership (§97: provider_id equals the caller's identity). An unscoped platform
     * authority keeps its legacy platform-wide access; every other unscoped caller is denied.
     */
    private boolean canAccessEvent(EventResponse event, HttpServletRequest request) {
        UUID callerInstitutionId = getInstitutionId(request);
        if (callerInstitutionId != null && callerInstitutionId.equals(event.getInstitutionId())) {
            return true;
        }
        String role = getRequestRole(request);
        if (callerInstitutionId == null && isPlatformRole(role)) {
            return true;
        }
        UUID callerUserId = getUserId(request);
        return callerUserId != null && callerUserId.toString().equals(event.getProviderId());
    }

    /**
     * §97: when an event carries provider_id, only the owning provider (or organizer/admin)
     * may mutate it. Provider-role callers always require ownership, so a null provider_id can
     * never be used to reach institution events they do not own.
     */
    private void assertProviderCanManage(EventResponse existing, HttpServletRequest request) {
        String role = getRequestRole(request);
        if (isAdminRole(role) || isPlatformRole(role)) {
            return;
        }
        UUID userId = getUserId(request);
        if (userId != null && userId.toString().equals(existing.getProviderId())) {
            return;
        }
        if (userId != null && userId.equals(existing.getOrganizerId())) {
            return;
        }
        if (isProviderRole(role)) {
            throw new ForbiddenException("Only the owning provider can manage this event");
        }
        if (existing.getProviderId() == null || existing.getProviderId().isBlank()) {
            return; // institution event without provider ownership — institution scope already verified
        }
        throw new ForbiddenException("Only the owning provider can manage this event");
    }

    private UUID getUserId(HttpServletRequest request) {
        Object userIdAttr = request.getAttribute("userId");
        if (userIdAttr instanceof UUID uuid) return uuid;
        return null;
    }

    private UUID getInstitutionId(HttpServletRequest request) {
        Object instIdAttr = request.getAttribute("institutionId");
        if (instIdAttr instanceof UUID uuid) return uuid;
        return null;
    }

    private String getRequestRole(HttpServletRequest request) {
        Object roleAttr = request.getAttribute("userRole");
        return roleAttr instanceof String role ? role : null;
    }

    private boolean isPlatformRole(String role) {
        return "ADMIN".equals(role) || "NATIONAL_ADMIN".equals(role);
    }

    private boolean isProviderRole(String role) {
        return "PROVIDER_ADMIN".equals(role) || "PROVIDER_STAFF".equals(role);
    }

    private boolean isAdminRole(String role) {
        return "ADMIN".equals(role) || "INSTITUTION_ADMIN".equals(role)
                || "NATIONAL_ADMIN".equals(role);
    }

    private boolean isLearnerRole(String role) {
        return "STUDENT".equals(role) || "OTHER_LEARNER".equals(role) || "LEARNER".equals(role);
    }
}
