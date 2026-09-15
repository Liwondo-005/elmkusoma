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

    public enum EventType {
        SEMINAR, WORKSHOP, WEBINAR, TRAINING, CONFERENCE, LECTURE, OTHER
    }

    public enum EventStatus {
        DRAFT, PUBLISHED, CANCELLED, COMPLETED
    }

    public enum EventCategory {
        ACADEMIC, TECHNICAL, PROFESSIONAL, COMMUNITY, CULTURAL, SPORTS, OTHER
    }
}
