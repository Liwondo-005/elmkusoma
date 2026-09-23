package tz.elmkusoma.event.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event API response.
 *
 * <p>Serialization / display contract (§96):
 * <ul>
 *   <li>{@code startsAt}/{@code endsAt}/{@code cancelledAt}/{@code createdAt} are naive
 *       {@link java.time.LocalDateTime} values serialized as ISO-8601 local date-time
 *       strings (Jackson {@code write-dates-as-timestamps=false}); they are wall-clock
 *       times in the institution/event {@link #timezone} (IANA zone id, defaults to
 *       {@code Africa/Dar_es_Salaam} at creation), <b>not</b> UTC instants.</li>
 *   <li>Clients must render using {@link #timezone}; conversions to UTC or the viewer's
 *       zone are client responsibilities. No offset/Z suffix is ever emitted.</li>
 *   <li>{@link #meetingUrl} is only present for registered participants (or
 *       organizer/provider/teacher/admin callers) — see §59.</li>
 * </ul>
 */
@Data
@Builder
public class EventResponse {
    private UUID id;
    private UUID institutionId;
    private UUID organizerId;
    private String organizerName;
    private String title;
    private String description;
    private String eventType;
    private String category;
    private String location;
    private String meetingUrl;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private Integer durationMinutes;
    private Integer maxParticipants;
    private Integer registeredCount;
    private Integer availableSpots;
    /** §69: true when registeredCount is within 10% of maxParticipants but not yet full. */
    private Boolean almostFull;
    private String status;
    private String eventStatus;
    private String accessLevel;
    private String timezone;
    private String thumbnailUrl;
    private String tags;
    private Boolean isFree;
    private Boolean requiresApproval;
    private Boolean isRegistered;
    private String registrationStatus;
    private Integer materialCount;
    private Boolean hasRecording;
    private LocalDateTime createdAt;
    private String eventFormat;
    private String difficulty;
    private String targetAudience;
    private String prerequisites;
    private String learningOutcomes;
    private String agenda;
    private LocalDateTime cancelledAt;
    private String cancellationReason;
    private UUID rescheduledFrom;
    private String recordingUrl;
    private String recordingStatus;
    private String providerId;
    private String presenterName;
    private UUID relatedCourseId;
    private UUID relatedModuleId;
    private UUID relatedLessonId;
}
