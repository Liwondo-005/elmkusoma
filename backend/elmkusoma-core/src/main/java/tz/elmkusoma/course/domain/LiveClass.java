package tz.elmkusoma.course.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "live_classes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class LiveClass extends BaseEntity {

    @Column(name = "subject_id")
    private UUID subjectId;

    @Column(name = "teacher_id", nullable = false)
    private UUID teacherId;

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes = 60;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "SCHEDULED";

    @Column(name = "max_participants")
    private Integer maxParticipants;

    @Column(name = "class_group_id")
    private UUID classGroupId;

    @Column(name = "recording_url", length = 500)
    private String recordingUrl;

    @Column(name = "recording_enabled")
    private Boolean recordingEnabled = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_type", length = 30)
    private LiveClassSessionType sessionType = LiveClassSessionType.LECTURE;

    @Column(name = "timezone", length = 50)
    private String timezone = "Africa/Dar_es_Salaam";

    @Column(name = "is_recurring")
    private Boolean isRecurring = false;

    @Column(name = "recurrence_pattern", length = 50)
    private String recurrencePattern;

    @Column(name = "recurrence_end_date")
    private java.time.LocalDate recurrenceEndDate;

    @Column(name = "parent_recurring_id")
    private UUID parentRecurringId;

    @Column(name = "lobby_enabled")
    private Boolean lobbyEnabled = false;

    public enum LiveClassStatus {
        SCHEDULED, STARTING, IN_PROGRESS, LIVE, ENDING, COMPLETED, ENDED, CANCELLED,
        SERVICE_DEGRADED, SERVICE_UNAVAILABLE, RECOVERING
    }
}
