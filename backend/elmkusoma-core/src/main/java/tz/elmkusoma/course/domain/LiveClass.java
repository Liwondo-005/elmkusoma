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

    @Column(name = "meeting_url", length = 500)
    private String meetingUrl;

    @Column(name = "max_participants")
    private Integer maxParticipants;

    @Column(name = "recording_url", length = 500)
    private String recordingUrl;

    public enum LiveClassStatus {
        SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED
    }
}
