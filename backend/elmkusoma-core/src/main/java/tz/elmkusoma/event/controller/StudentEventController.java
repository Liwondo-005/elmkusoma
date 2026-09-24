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
    public ResponseEntity<ApiResponse<List<EventResponse>>> getUpcomingEvents(
            HttpServletRequest request,
            @RequestParam(required = false) Boolean personalized,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        UUID institutionId = getInstitutionId(request);
        UUID userId = getUserId(request);
        String role = getRole(request);

        // §53: personal relevance ranking for learners
        if (Boolean.TRUE.equals(personalized) && userId != null && isLearnerRole(role)) {
            return ResponseEntity.ok(ApiResponse.success(
                    sanitizeMeetingUrls(eventService.getPersonalizedEvents(institutionId, userId), userId, role)));
        }

        // §81/§82: optional Pageable pagination (absent page/size keeps full list)
        if (page != null || size != null) {
            org.springframework.data.domain.Page<EventResponse> result =
                    eventService.getEvents(institutionId, "PUBLISHED", null, null, null,
                            org.springframework.data.domain.PageRequest.of(
                                    page != null && page >= 0 ? page : 0,
                                    size != null && size > 0 ? size : 20,
                                    org.springframework.data.domain.Sort.by("startsAt").ascending()));
            return ResponseEntity.ok()
                    .header("X-Total-Count", String.valueOf(result.getTotalElements()))
                    .body(ApiResponse.success(sanitizeMeetingUrls(result.getContent(), userId, role)));
        }
        List<EventResponse> events = eventService.getUpcomingEvents(institutionId);
        return ResponseEntity.ok(ApiResponse.success(sanitizeMeetingUrls(events, userId, role)));
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

    private String getRole(HttpServletRequest request) {
        Object roleAttr = request.getAttribute("userRole");
        return roleAttr instanceof String role ? role : null;
    }

    private boolean isLearnerRole(String role) {
        return "STUDENT".equals(role) || "OTHER_LEARNER".equals(role) || "LEARNER".equals(role);
    }

    /** §59: meeting/join URL only for registered learners (staff/admins keep full data). */
    private List<EventResponse> sanitizeMeetingUrls(List<EventResponse> events, UUID userId, String role) {
        if (events == null || !isLearnerRole(role)) {
            return events;
        }
        for (EventResponse e : events) {
            if (e.getMeetingUrl() == null) {
                continue;
            }
            boolean registered = userId != null
                    && (Boolean.TRUE.equals(e.getIsRegistered())
                            || eventService.isUserRegistered(e.getId(), userId));
            if (!registered) {
                e.setMeetingUrl(null);
            }
        }
        return events;
    }

    private UUID getInstitutionId(HttpServletRequest request) {
        Object instIdAttr = request.getAttribute("institutionId");
        if (instIdAttr instanceof UUID uuid) return uuid;
        return null;
    }
}
