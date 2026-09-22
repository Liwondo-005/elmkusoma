package tz.elmkusoma.event.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.event.dto.*;
import tz.elmkusoma.event.service.EventService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/events")
@PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
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
            @RequestParam(required = false) String category) {
        UUID institutionId = getInstitutionId(request);
        List<EventResponse> events = eventService.getEvents(institutionId, status, eventType, category);
        return ResponseEntity.ok(ApiResponse.success(events));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EventResponse>> getEvent(
            @PathVariable UUID id,
            HttpServletRequest request) {
        EventResponse event = eventService.getEventByIdForAdmin(id);
        return ResponseEntity.ok(ApiResponse.success(event));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EventResponse>> createEvent(
            @Valid @RequestBody EventRequest request,
            HttpServletRequest httpRequest) {
        UUID institutionId = getInstitutionId(httpRequest);
        UUID userId = getUserId(httpRequest);
        EventResponse event = eventService.createEvent(institutionId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Event created", event));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EventResponse>> updateEvent(
            @PathVariable UUID id,
            @Valid @RequestBody EventRequest request,
            HttpServletRequest httpRequest) {
        UUID institutionId = getInstitutionId(httpRequest);
        EventResponse existing = eventService.getEventByIdForAdmin(id);
        if (institutionId != null && !institutionId.equals(existing.getInstitutionId())) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
        }
        EventResponse event = eventService.updateEvent(id, request);
        return ResponseEntity.ok(ApiResponse.success("Event updated", event));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteEvent(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {
        UUID institutionId = getInstitutionId(httpRequest);
        EventResponse existing = eventService.getEventByIdForAdmin(id);
        if (institutionId != null && !institutionId.equals(existing.getInstitutionId())) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
        }
        eventService.deleteEvent(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/{id}/registrations")
    public ResponseEntity<ApiResponse<List<EventRegistrationResponse>>> getEventRegistrations(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {
        UUID userId = getUserId(httpRequest);
        UUID institutionId = getInstitutionId(httpRequest);
        EventResponse existing = eventService.getEventByIdForAdmin(id);
        if (institutionId != null && !institutionId.equals(existing.getInstitutionId())) {
            return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
        }
        List<EventRegistrationResponse> registrations = eventService.getEventRegistrations(id, userId);
        return ResponseEntity.ok(ApiResponse.success(registrations));
    }

    @GetMapping("/{id}/materials")
    public ResponseEntity<ApiResponse<List<EventMaterialResponse>>> getEventMaterials(
            @PathVariable UUID id) {
        List<EventMaterialResponse> materials = eventService.getEventMaterials(id);
        return ResponseEntity.ok(ApiResponse.success(materials));
    }

    @PostMapping("/{id}/materials")
    public ResponseEntity<ApiResponse<EventMaterialResponse>> addEventMaterial(
            @PathVariable UUID id,
            @Valid @RequestBody EventMaterialRequest request,
            HttpServletRequest httpRequest) {
        UUID userId = getUserId(httpRequest);
        request.setEventId(id);
        EventMaterialResponse material = eventService.addEventMaterial(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Material added", material));
    }

    @DeleteMapping("/materials/{materialId}")
    public ResponseEntity<ApiResponse<Void>> deleteEventMaterial(
            @PathVariable UUID materialId,
            HttpServletRequest httpRequest) {
        eventService.deleteEventMaterial(materialId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<EventResponse>> publishEvent(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {
        UUID institutionId = getInstitutionId(httpRequest);
        EventResponse event = eventService.publishEvent(id, institutionId);
        return ResponseEntity.ok(ApiResponse.success("Event published", event));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<EventResponse>> cancelEvent(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body,
            HttpServletRequest httpRequest) {
        UUID institutionId = getInstitutionId(httpRequest);
        String reason = body != null ? body.getOrDefault("reason", "") : "";
        EventResponse event = eventService.cancelEvent(id, institutionId, reason);
        return ResponseEntity.ok(ApiResponse.success("Event cancelled", event));
    }

    @PostMapping("/{id}/start-live")
    public ResponseEntity<ApiResponse<EventResponse>> startLiveEvent(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {
        UUID institutionId = getInstitutionId(httpRequest);
        EventResponse event = eventService.startLiveEvent(id, institutionId);
        return ResponseEntity.ok(ApiResponse.success("Event started live", event));
    }

    @PostMapping("/{id}/end-live")
    public ResponseEntity<ApiResponse<EventResponse>> endLiveEvent(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {
        UUID institutionId = getInstitutionId(httpRequest);
        EventResponse event = eventService.endLiveEvent(id, institutionId);
        return ResponseEntity.ok(ApiResponse.success("Event ended", event));
    }

    @GetMapping("/{id}/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getEventSummary(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {
        UUID institutionId = getInstitutionId(httpRequest);
        Map<String, Object> summary = eventService.getEventSummary(id, institutionId);
        return ResponseEntity.ok(ApiResponse.success(summary));
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
}
