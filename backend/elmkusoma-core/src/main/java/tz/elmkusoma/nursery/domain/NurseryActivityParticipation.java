package tz.elmkusoma.nursery.domain;

import jakarta.persistence.*;
import lombok.*;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "nursery_activity_participations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NurseryActivityParticipation extends BaseEntity {

    @Column(name = "activity_id", nullable = false)
    private UUID activityId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "participation_level")
    @Enumerated(EnumType.STRING)
    private ParticipationLevel participationLevel;

    @Column(name = "engagement_score")
    private Integer engagementScore;

    @Column(name = "notes")
    private String notes;

    @Column(name = "participated_at", nullable = false)
    private LocalDateTime participatedAt;

    public enum ParticipationLevel {
        FULL,
        PARTIAL,
        MINIMAL,
        NONE
    }
}