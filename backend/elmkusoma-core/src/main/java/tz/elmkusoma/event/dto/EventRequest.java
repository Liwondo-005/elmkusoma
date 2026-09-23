package tz.elmkusoma.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class EventRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotBlank(message = "Event type is required")
    private String eventType;

    private String category;

    private String location;

    private String meetingUrl;

    @NotNull(message = "Start time is required")
    private String startsAt;

    private String endsAt;

    private Integer durationMinutes = 60;

    private Integer maxParticipants;

    private String status = "DRAFT";

    private String thumbnailUrl;

    private String tags;

    private Boolean isFree = true;

    private Boolean requiresApproval = false;

    private UUID organizerId;

    private String timezone;

    private String accessLevel;

    private String presenterName;

    private String eventFormat;
    private String difficulty;
    private String targetAudience;
    private String prerequisites;
    private String learningOutcomes;
    private String agenda;
    private UUID rescheduledFrom;

    /** Optional links to course/module/lesson content (persisted on Event, §46/§48/§51/§52). */
    private UUID relatedCourseId;
    private UUID relatedModuleId;
    private UUID relatedLessonId;

    /** Owning provider id (stored on Event and enforced on mutation, §97). */
    private String providerId;
}
