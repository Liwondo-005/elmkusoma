package tz.elmkusoma.highereducation.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "workshop_sessions")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class WorkshopSession extends BaseEntity {

    @Column(name = "institution_id", nullable = false)
    private UUID institutionId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "workshop_type", length = 30, nullable = false)
    @Builder.Default
    private WorkshopType workshopType = WorkshopType.WORKSHOP;

    @Column(name = "course_id")
    private UUID courseId;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "location", length = 200)
    private String location;

    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private WorkshopStatus status = WorkshopStatus.SCHEDULED;

    @Column(name = "max_participants")
    private Integer maxParticipants;

    @Column(name = "current_participants")
    @Builder.Default
    private Integer currentParticipants = 0;

    @Column(name = "materials_url", length = 500)
    private String materialsUrl;

    @Column(name = "instructor_id")
    private UUID instructorId;
}
