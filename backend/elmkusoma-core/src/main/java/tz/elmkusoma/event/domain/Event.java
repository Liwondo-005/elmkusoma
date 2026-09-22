package tz.elmkusoma.event.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Event extends BaseEntity {

    @Column(name = "organizer_id", nullable = false)
    private UUID organizerId;

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "location", length = 500)
    private String location;

    @Column(name = "meeting_url", length = 500)
    private String meetingUrl;

    @Column(name = "starts_at", nullable = false)
    private java.time.LocalDateTime startsAt;

    @Column(name = "ends_at")
    private java.time.LocalDateTime endsAt;

    @Column(name = "duration_minutes")
    private Integer durationMinutes = 60;

    @Column(name = "max_participants")
    private Integer maxParticipants;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "DRAFT";

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "tags", length = 500)
    private String tags;

    @Column(name = "is_free", nullable = false)
    private Boolean isFree = true;

    @Column(name = "requires_approval", nullable = false)
    private Boolean requiresApproval = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_status", length = 30)
    private EventStatus eventStatus = EventStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type_enum", length = 50)
    private EventType eventTypeEnum;

    @Column(name = "timezone", length = 50)
    private String timezone;

    @Column(name = "max_capacity")
    private Integer maxCapacity;

    @Column(name = "current_registrations")
    private Integer currentRegistrations = 0;

    @Column(name = "related_course_id", length = 36)
    private String relatedCourseId;

    @Column(name = "related_module_id", length = 36)
    private String relatedModuleId;

    @Column(name = "related_lesson_id", length = 36)
    private String relatedLessonId;

    @Column(name = "recording_url", length = 500)
    private String recordingUrl;

    @Column(name = "recording_status", length = 20)
    private String recordingStatus;

    @Column(name = "provider_id", length = 36)
    private String providerId;

    @Column(name = "presenter_name", length = 200)
    private String presenterName;

    @Column(name = "event_format", length = 30)
    private String eventFormat;

    @Column(name = "difficulty", length = 30)
    private String difficulty;

    @Column(name = "target_audience", length = 100)
    private String targetAudience;

    @Column(name = "prerequisites", columnDefinition = "TEXT")
    private String prerequisites;

    @Column(name = "learning_outcomes", columnDefinition = "TEXT")
    private String learningOutcomes;

    @Column(name = "agenda", columnDefinition = "TEXT")
    private String agenda;

    @Column(name = "cancelled_at")
    private java.time.LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "rescheduled_from")
    private java.time.LocalDateTime rescheduledFrom;

    @Column(name = "access_level", length = 30)
    private String accessLevel;

    public enum EventCategory {
        ACADEMIC, TECHNICAL, PROFESSIONAL, COMMUNITY, CULTURAL, SPORTS, OTHER
    }
}
