package tz.elmkusoma.highereducation.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "learning_collaborations")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class LearningCollaboration extends BaseEntity {

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "peer_student_id")
    private UUID peerStudentId;

    @Column(name = "collaboration_type", length = 30, nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private CollaborationType collaborationType = CollaborationType.STUDY_GROUP;

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "course_id")
    private UUID courseId;

    @Column(name = "status", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private CollaborationStatus status = CollaborationStatus.ACTIVE;
}
