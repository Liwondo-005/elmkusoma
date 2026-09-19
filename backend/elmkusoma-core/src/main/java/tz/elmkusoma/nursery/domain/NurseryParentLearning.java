package tz.elmkusoma.nursery.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "nursery_parent_learning")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class NurseryParentLearning extends BaseEntity {

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "class_group_id", nullable = false)
    private UUID classGroupId;

    @Column(name = "activity_title", nullable = false)
    private String activityTitle;

    @Column(name = "activity_description")
    private String activityDescription;

    @Column(name = "activity_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ActivityType activityType;

    @Column(name = "parent_name")
    private String parentName;

    @Column(name = "completion_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private CompletionStatus completionStatus = CompletionStatus.PENDING;

    @Column(name = "completed_date")
    private LocalDate completedDate;

    @Column(name = "notes")
    private String notes;

    public enum ActivityType {
        READING, COUNTING, COOKING, GARDENING, SINGING, CRAFTING
    }

    public enum CompletionStatus {
        PENDING, IN_PROGRESS, COMPLETED
    }
}
