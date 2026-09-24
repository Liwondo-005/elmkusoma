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
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean personalized,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        UUID institutionId = getInstitutionId(request);
        UUID userId = getUserId(request);

        // §53: personal relevance ranking from enrollments + previous participation
        if (Boolean.TRUE.equals(personalized) && userId != null
                && (search == null || search.isBlank())) {
            List<EventResponse> ranked = eventService.getPersonalizedEvents(institutionId, userId);
            return ResponseEntity.ok(ApiResponse.success(sanitizeMeetingUrls(ranked, userId)));
        }

        if (search != null && !search.isBlank()) {
            List<EventResponse> events = eventService.searchEvents(institutionId, search);
            return ResponseEntity.ok(ApiResponse.success(sanitizeMeetingUrls(events, userId)));
        }

        // §81/§82: optional Pageable pagination (absent page/size keeps full list)
        if (page != null || size != null) {
            org.springframework.data.domain.Page<EventResponse> result =
                    eventService.getEvents(institutionId, "PUBLISHED", eventType, category, null,
                            org.springframework.data.domain.PageRequest.of(
                                    page != null && page >= 0 ? page : 0,
                                    size != null && size > 0 ? size : 20,
                                    org.springframework.data.domain.Sort.by("startsAt").ascending()));
            return ResponseEntity.ok()
                    .header("X-Total-Count", String.valueOf(result.getTotalElements()))
                    .body(ApiResponse.success(sanitizeMeetingUrls(result.getContent(), userId)));
        }
        List<EventResponse> events = eventService.getEvents(institutionId, "PUBLISHED", eventType, category);
        return ResponseEntity.ok(ApiResponse.success(sanitizeMeetingUrls(events, userId)));
    }

    @GetMapping("/events/upcoming")
    public ResponseEntity<ApiResponse<List<EventResponse>>> getUpcomingEvents(
            HttpServletRequest request,
            @RequestParam(required = false) Boolean personalized,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        UUID institutionId = getInstitutionId(request);
        UUID userId = getUserId(request);
        if (Boolean.TRUE.equals(personalized) && userId != null) {
            return ResponseEntity.ok(ApiResponse.success(
                    sanitizeMeetingUrls(eventService.getPersonalizedEvents(institutionId, userId), userId)));
        }
        if (page != null || size != null) {
            org.springframework.data.domain.Page<EventResponse> result =
                    eventService.getUpcomingEvents(institutionId,
                            org.springframework.data.domain.PageRequest.of(
                                    page != null && page >= 0 ? page : 0,
                                    size != null && size > 0 ? size : 20,
                                    org.springframework.data.domain.Sort.by("startsAt").ascending()));
            return ResponseEntity.ok()
                    .header("X-Total-Count", String.valueOf(result.getTotalElements()))
                    .body(ApiResponse.success(sanitizeMeetingUrls(result.getContent(), userId)));
        }
        List<EventResponse> events = eventService.getUpcomingEvents(institutionId);
        return ResponseEntity.ok(ApiResponse.success(sanitizeMeetingUrls(events, userId)));
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

    @GetMapping("/events/{id}/calendar")
    public ResponseEntity<String> exportEventCalendar(@PathVariable UUID id, HttpServletRequest request) {
        UUID userId = getUserId(request);
        EventResponse event = eventService.getEventById(id, userId);

        java.time.LocalDateTime start = event.getStartsAt();
        java.time.LocalDateTime end = event.getEndsAt() != null
                ? event.getEndsAt()
                : start.plusMinutes(event.getDurationMinutes() != null ? event.getDurationMinutes() : 60);

        String location = event.getLocation() != null && !event.getLocation().isBlank()
                ? event.getLocation()
                : (event.getMeetingUrl() != null && !event.getMeetingUrl().isBlank()
                        ? event.getMeetingUrl()
                        : "ELMKUSOMA");
        String description = event.getDescription() != null ? event.getDescription() : "";
        if (event.getCancellationReason() != null && !event.getCancellationReason().isBlank()) {
            description = "CANCELLED: " + event.getCancellationReason()
                    + (description.isBlank() ? "" : " - " + description);
        }
        String eventUrl = request.getRequestURL().toString().replace("/calendar", "");
        String statusLine = "CANCELLED".equalsIgnoreCase(event.getStatus()) ? "CANCELLED" : "CONFIRMED";

        String ics = "BEGIN:VCALENDAR\r\n"
                + "VERSION:2.0\r\n"
                + "PRODID:-//ELMKUSOMA//Events//EN\r\n"
                + "CALSCALE:GREGORIAN\r\n"
                + "BEGIN:VEVENT\r\n"
                + "UID:" + event.getId() + "@elmkusoma\r\n"
                + "DTSTAMP:" + formatIcsDateTime(java.time.LocalDateTime.now()) + "\r\n"
                + "DTSTART:" + formatIcsDateTime(start) + "\r\n"
                + "DTEND:" + formatIcsDateTime(end) + "\r\n"
                + "SUMMARY:" + escapeIcs(event.getTitle()) + "\r\n"
                + "DESCRIPTION:" + escapeIcs(description) + "\r\n"
                + "LOCATION:" + escapeIcs(location) + "\r\n"
                + "URL:" + eventUrl + "\r\n"
                + "STATUS:" + statusLine + "\r\n"
                + "END:VEVENT\r\n"
                + "END:VCALENDAR\r\n";

        return ResponseEntity.ok()
                .header("Content-Type", "text/calendar; charset=utf-8")
                .header("Content-Disposition", "attachment; filename=\"event-" + event.getId() + ".ics\"")
                .body(ics);
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

    /**
     * §49: public materials for an event. Each item carries a stable material {@code id}
     * and {@code fileUrl} so downstream consumers (e.g. D04 deep links) can reference
     * materials directly — no separate linking platform is introduced here.
     */
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

    /** §59: strip meeting/join URLs from list payloads for users who are not registered. */
    private List<EventResponse> sanitizeMeetingUrls(List<EventResponse> events, UUID userId) {
        if (events == null) {
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

    private String formatIcsDateTime(java.time.LocalDateTime dateTime) {
        return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"));
    }

    private String escapeIcs(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\").replace(",", "\\,").replace(";", "\\;").replace("\n", "\\n");
    }
}
