package tz.elmkusoma.event.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.event.domain.Event;
import tz.elmkusoma.event.domain.EventStatus;
import tz.elmkusoma.event.dto.EventJoinResponse;
import tz.elmkusoma.event.repository.EventRegistrationRepository;
import tz.elmkusoma.event.repository.EventRepository;
import tz.elmkusoma.event.service.EventService;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.liveclass.service.LiveKitService;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;

import java.util.Set;
import java.util.UUID;

/**
 * Learner event join endpoint (§90/§99/§104): exchanges authorization for a
 * short-lived LiveKit token bound to room {@code event-<eventId>}.
 *
 * <p>Status rules (§66/§91): LIVE/STARTING join fully; PREPARING issues a
 * waiting-room token; CANCELLED/RESCHEDULED/FAILED and ended states are blocked;
 * anything else is not yet joinable (409). Access follows §59: registered
 * participants, organizer/provider/teacher/admin, or a PUBLIC event that is live.
 */
@RestController
@RequestMapping("/v1/learner/events")
@PreAuthorize("hasAnyRole('STUDENT','OTHER_LEARNER','TEACHER')")
public class LearnerEventJoinController {

    private static final Set<EventStatus> JOINABLE = Set.of(EventStatus.LIVE, EventStatus.STARTING);
    private static final Set<EventStatus> BLOCKED = Set.of(
            EventStatus.CANCELLED, EventStatus.RESCHEDULED, EventStatus.FAILED,
            EventStatus.ENDED, EventStatus.RECORDING, EventStatus.PROCESSING,
            EventStatus.REPLAY_AVAILABLE, EventStatus.DRAFT, EventStatus.REVIEW);

    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final EventService eventService;
    private final LiveKitService liveKitService;

    public LearnerEventJoinController(EventRepository eventRepository,
                                      EventRegistrationRepository registrationRepository,
                                      UserRepository userRepository,
                                      EventService eventService,
                                      LiveKitService liveKitService) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.userRepository = userRepository;
        this.eventService = eventService;
        this.liveKitService = liveKitService;
    }

    @PostMapping("/{eventId}/join")
    public ResponseEntity<ApiResponse<EventJoinResponse>> joinEvent(
            @PathVariable UUID eventId,
            @RequestAttribute(value = "userId", required = false) UUID userId,
            @RequestAttribute(value = "institutionId", required = false) UUID institutionId,
            @RequestAttribute(value = "userRole", required = false) String userRole,
            @RequestHeader(value = "X-Institution-Id", required = false) UUID headerInstitutionId) {

        if (userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Authentication required"));
        }

        Event event = eventRepository.findById(eventId)
                .filter(e -> !Boolean.TRUE.equals(e.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        UUID callerInstitutionId = institutionId != null ? institutionId : headerInstitutionId;
        if (callerInstitutionId == null || event.getInstitutionId() == null
                || !event.getInstitutionId().equals(callerInstitutionId)) {
            throw new ForbiddenException("Access denied");
        }

        EventStatus status = event.getEventStatus() != null
                ? event.getEventStatus()
                : EventStatus.fromString(event.getStatus());
        String statusName = status.name();

        if (BLOCKED.contains(status)) {
            throw new IllegalStateException(
                    status == EventStatus.CANCELLED || status == EventStatus.RESCHEDULED
                            ? "This event has been " + statusName.toLowerCase() + " - join is not possible"
                            : "This event is not joinable (" + statusName + ")");
        }
        boolean waitingRoom = status == EventStatus.PREPARING;
        if (!JOINABLE.contains(status) && !waitingRoom) {
            throw new IllegalStateException("This event has not started yet (" + statusName + ")");
        }

        boolean registered = registrationRepository
                .existsByEventIdAndUserIdAndIsDeletedFalse(eventId, userId);
        boolean staff = isStaff(userId, userRole, event);
        String accessLevel = event.getAccessLevel() == null || event.getAccessLevel().isBlank()
                ? "INSTITUTION"
                : event.getAccessLevel().trim().toUpperCase();
        boolean publicLive = ("PUBLIC".equals(accessLevel) || "AUTHENTICATED".equals(accessLevel))
                && JOINABLE.contains(status);

        if (!registered && !staff && !publicLive) {
            throw new ForbiddenException("Registration required to join this event");
        }

        boolean isTeacher = staff && !"STUDENT".equals(userRole) && !"OTHER_LEARNER".equals(userRole);
        String token = liveKitService.generateEventToken(eventId, userId, userId.toString(), isTeacher);
        String roomName = liveKitService.generateRoomNameForEvent(eventId);

        // §59: external meeting URL only for registered/staff callers
        String meetingUrl = (registered || staff) ? event.getMeetingUrl() : null;

        // Attendance path: record intent when the learner is registered and the event is live
        if (registered && JOINABLE.contains(status)) {
            try {
                eventService.markEventAttendance(eventId, userId);
            } catch (Exception ignored) {
                // attendance is best-effort — join must still succeed
            }
        }

        EventJoinResponse response = EventJoinResponse.builder()
                .token(token)
                .serverUrl(liveKitService.isAvailable() ? liveKitService.getServerUrl() : null)
                .roomName(roomName)
                .meetingUrl(meetingUrl)
                .eventStatus(statusName)
                .liveKitAvailable(liveKitService.isAvailable())
                .waitingRoom(waitingRoom)
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private boolean isStaff(UUID userId, String userRole, Event event) {
        if (userId.equals(event.getOrganizerId())) {
            return true;
        }
        if (event.getProviderId() != null && userId.toString().equals(event.getProviderId())) {
            return true;
        }
        if ("TEACHER".equals(userRole) || "ADMIN".equals(userRole)
                || "INSTITUTION_ADMIN".equals(userRole) || "NATIONAL_ADMIN".equals(userRole)) {
            return true;
        }
        return userRepository.findById(userId)
                .map(u -> u.getRole() == User.Role.ADMIN
                        || u.getRole() == User.Role.NATIONAL_ADMIN
                        || u.getRole() == User.Role.INSTITUTION_ADMIN
                        || u.getRole() == User.Role.TEACHER
                        || u.getRole() == User.Role.PROVIDER_ADMIN
                        || u.getRole() == User.Role.PROVIDER_STAFF)
                .orElse(false);
    }
}
