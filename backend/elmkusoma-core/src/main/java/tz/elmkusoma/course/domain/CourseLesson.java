package tz.elmkusoma.course.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "course_lessons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CourseLesson extends BaseEntity {

    @Column(name = "module_id", nullable = false)
    private UUID moduleId;

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Column(name = "content_url", length = 500)
    private String contentUrl;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "is_free", nullable = false)
    private Boolean isFree = false;

    public enum ContentType {
        VIDEO, DOCUMENT, QUIZ, ASSIGNMENT, LINK, TEXT
    }
}
