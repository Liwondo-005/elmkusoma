package tz.elmkusoma.event.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.event.dto.*;
import tz.elmkusoma.event.service.EventService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/learner")
@PreAuthorize("hasRole('OTHER_LEARNER')")
public class LearnerEventController {

    private final EventService eventService;

    public LearnerEventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping("/events")
    public ResponseEntity<ApiResponse<List<EventResponse>>> getEvents(
            HttpServletRequest request,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search) {
        UUID institutionId = getInstitutionId(request);
        if (search != null && !search.isBlank()) {
            List<EventResponse> events = eventService.searchEvents(institutionId, search);
            return ResponseEntity.ok(ApiResponse.success(events));
        }
        List<EventResponse> events = eventService.getEvents(institutionId, "PUBLISHED", eventType, category);
        return ResponseEntity.ok(ApiResponse.success(events));
    }

    @GetMapping("/events/upcoming")
    public ResponseEntity<ApiResponse<List<EventResponse>>> getUpcomingEvents(HttpServletRequest request) {
        UUID institutionId = getInstitutionId(request);
        List<EventResponse> events = eventService.getUpcomingEvents(institutionId);
        return ResponseEntity.ok(ApiResponse.success(events));
    }

    @GetMapping("/events/past")
    public ResponseEntity<ApiResponse<List<EventResponse>>> getPastEvents(HttpServletRequest request) {
        UUID institutionId = getInstitutionId(request);
        List<EventResponse> events = eventService.getPastEvents(institutionId);
        return ResponseEntity.ok(ApiResponse.success(events));
    }

    @GetMapping("/events/{id}")
    public ResponseEntity<ApiResponse<EventResponse>> getEvent(@PathVariable UUID id, HttpServletRequest request) {
        UUID userId = getUserId(request);
        EventResponse event = eventService.getEventById(id, userId);
        return ResponseEntity.ok(ApiResponse.success(event));
    }

    @PostMapping("/events/{id}/register")
    public ResponseEntity<ApiResponse<EventRegistrationResponse>> registerForEvent(
            @PathVariable UUID id, HttpServletRequest request) {
        UUID userId = getUserId(request);
        EventRegistrationResponse registration = eventService.registerForEvent(id, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful", registration));
    }

    @PostMapping("/events/{id}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelRegistration(
            @PathVariable UUID id,
            @RequestBody(required = false) java.util.Map<String, String> body,
            HttpServletRequest request) {
        UUID userId = getUserId(request);
        String reason = body != null ? body.get("reason") : null;
        eventService.cancelRegistration(id, userId, reason);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/events/registered")
    public ResponseEntity<ApiResponse<List<EventResponse>>> getRegisteredEvents(HttpServletRequest request) {
        UUID userId = getUserId(request);
        UUID institutionId = getInstitutionId(request);
        List<EventResponse> events = eventService.getRegisteredEvents(userId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(events));
    }

    @GetMapping("/events/registered/past")
    public ResponseEntity<ApiResponse<List<EventResponse>>> getRegisteredPastEvents(HttpServletRequest request) {
        UUID userId = getUserId(request);
        UUID institutionId = getInstitutionId(request);
        List<EventResponse> events = eventService.getRegisteredPastEvents(userId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(events));
    }

    @GetMapping("/events/{id}/materials")
    public ResponseEntity<ApiResponse<List<EventMaterialResponse>>> getEventMaterials(@PathVariable UUID id) {
        List<EventMaterialResponse> materials = eventService.getPublicEventMaterials(id);
        return ResponseEntity.ok(ApiResponse.success(materials));
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
