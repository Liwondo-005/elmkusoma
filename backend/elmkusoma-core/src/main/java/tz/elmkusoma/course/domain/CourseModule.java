package tz.elmkusoma.course.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "course_modules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CourseModule extends BaseEntity {

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    /**
     * Module code (e.g. "CS 201"). Nullable on purpose: university-style codes apply to
     * programme modules, while standalone provider courses must not be forced into them.
     */
    @Column(name = "module_code", length = 50)
    private String moduleCode;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
