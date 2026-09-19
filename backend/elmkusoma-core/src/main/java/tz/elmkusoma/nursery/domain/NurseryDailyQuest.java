package tz.elmkusoma.nursery.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "nursery_daily_quests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class NurseryDailyQuest extends BaseEntity {

    @Column(name = "class_group_id", nullable = false)
    private UUID classGroupId;

    @Column(name = "student_id")
    private UUID studentId;

    @Column(name = "quest_title", nullable = false)
    private String questTitle;

    @Column(name = "quest_description")
    private String questDescription;

    @Column(name = "quest_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private QuestType questType;

    @Column(name = "reward_points")
    private Integer rewardPoints = 0;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private QuestStatus status = QuestStatus.PENDING;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "completed_date")
    private LocalDate completedDate;

    public enum QuestType {
        READING, MATH, SCIENCE, ART, PHYSICAL, LANGUAGE
    }

    public enum QuestStatus {
        PENDING, IN_PROGRESS, COMPLETED, FAILED
    }
}
