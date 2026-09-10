package tz.elmkusoma.course.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;
import tz.elmkusoma.academic.domain.EducationLevel;

import java.util.UUID;

@Entity
@Table(name = "courses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Course extends BaseEntity {

    @Column(name = "subject_id")
    private UUID subjectId;

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "level", nullable = false, length = 50)
    private String level;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "is_published", nullable = false)
    private Boolean isPublished = false;

    @Column(name = "is_featured", nullable = false)
    private Boolean isFeatured = false;

    public enum CourseLevel {
        NURSERY, PRIMARY, SECONDARY, COLLEGE, VETA, UNIVERSITY, ALL_LEVELS
    }
}
