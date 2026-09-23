package tz.elmkusoma.event.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.event.dto.*;
import tz.elmkusoma.event.service.EventService;

import java.util.List;
import java.util.UUID;

@RestController
// Both prefixes are exposed: the frontend rewrites /api/v1/* -> /v1/* while some
// clients call the /api/v1/* form directly against the backend base URL.
@RequestMapping({"/api/v1/student/events", "/v1/student/events"})
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER')")
public class StudentEventController {

    private final EventService eventService;

    @GetMapping
    // Read-only, institution-scoped list also used by the dashboard sidebar badge for
    // every role - the class-level student-only restriction does not apply here.
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'TEACHER', 'ADMIN', 'INSTITUTION_ADMIN', 'PARENT')")
    public ResponseEntity<ApiResponse<List<EventResponse>>> getUpcomingEvents(HttpServletRequest request) {
        UUID institutionId = getInstitutionId(request);
        List<EventResponse> events = eventService.getUpcomingEvents(institutionId);
        return ResponseEntity.ok(ApiResponse.success(events));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EventResponse>> getEvent(@PathVariable UUID id, HttpServletRequest request) {
        UUID userId = getUserId(request);
        EventResponse event = eventService.getEventById(id, userId);
        return ResponseEntity.ok(ApiResponse.success(event));
    }

    @GetMapping("/registered")
    public ResponseEntity<ApiResponse<List<EventResponse>>> getRegisteredEvents(HttpServletRequest request) {
        UUID userId = getUserId(request);
        UUID institutionId = getInstitutionId(request);
        List<EventResponse> events = eventService.getRegisteredEvents(userId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(events));
    }

    @PostMapping("/{id}/register")
    public ResponseEntity<ApiResponse<EventRegistrationResponse>> registerForEvent(
            @PathVariable UUID id, HttpServletRequest request) {
        UUID userId = getUserId(request);
        EventRegistrationResponse response = eventService.registerForEvent(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Registered for event", response));
    }

    @PostMapping("/{id}/cancel-registration")
    public ResponseEntity<ApiResponse<Void>> cancelRegistration(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "Student cancelled") String reason,
            HttpServletRequest request) {
        UUID userId = getUserId(request);
        eventService.cancelRegistration(id, userId, reason);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/{id}/materials")
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
