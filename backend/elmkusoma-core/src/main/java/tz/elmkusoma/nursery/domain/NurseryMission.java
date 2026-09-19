package tz.elmkusoma.nursery.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "nursery_missions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class NurseryMission extends BaseEntity {

    @Column(name = "class_group_id", nullable = false)
    private UUID classGroupId;

    @Column(name = "student_id")
    private UUID studentId;

    @Column(name = "mission_title", nullable = false)
    private String missionTitle;

    @Column(name = "mission_description")
    private String missionDescription;

    @Column(name = "mission_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private MissionType missionType;

    @Column(name = "reward_points")
    private Integer rewardPoints = 0;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private MissionStatus status = MissionStatus.PENDING;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "completed_date")
    private LocalDate completedDate;

    @Column(name = "evidence_notes")
    private String evidenceNotes;

    @Column(name = "evidence_image_url")
    private String evidenceImageUrl;

    public enum MissionType {
        HOME, COMMUNITY, NATURE, CREATIVITY
    }

    public enum MissionStatus {
        PENDING, IN_PROGRESS, COMPLETED, FAILED
    }
}
