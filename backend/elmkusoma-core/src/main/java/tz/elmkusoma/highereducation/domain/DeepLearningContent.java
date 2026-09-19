package tz.elmkusoma.highereducation.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "deep_learning_contents")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class DeepLearningContent extends BaseEntity {

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "course_id")
    private UUID courseId;

    @Column(name = "module_id")
    private UUID moduleId;

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "content_type", length = 30, nullable = false)
    @Builder.Default
    private DeepContentType contentType = DeepContentType.ARTICLE;

    @Column(name = "content_text", columnDefinition = "TEXT")
    private String contentText;

    @Column(name = "file_url", length = 500)
    private String fileUrl;

    @Column(name = "difficulty_level", length = 20)
    @Builder.Default
    private String difficultyLevel = "INTERMEDIATE";

    @Column(name = "tags", length = 500)
    private String tags;

    @Column(name = "is_completed")
    @Builder.Default
    private Boolean isCompleted = false;

    @Column(name = "time_spent_minutes")
    @Builder.Default
    private Integer timeSpentMinutes = 0;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
