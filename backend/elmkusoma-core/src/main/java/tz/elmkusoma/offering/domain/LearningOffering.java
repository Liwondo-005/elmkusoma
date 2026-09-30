package tz.elmkusoma.offering.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.academic.domain.EducationLevel;
import tz.elmkusoma.common.BaseEntity;
import tz.elmkusoma.learning.domain.Resource;

import java.util.UUID;

@Entity
@Table(name = "learning_offerings")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class LearningOffering extends BaseEntity {

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(name = "teacher_id")
    private UUID teacherId;

    @Column(name = "course_id")
    private UUID courseId;

    @Column(name = "subject_id")
    private UUID subjectId;

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "education_level", length = 50)
    private EducationLevel educationLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 20)
    private Resource.ResourceVisibility visibility = Resource.ResourceVisibility.INSTITUTION;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "DRAFT";

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_PUBLISHED = "PUBLISHED";
}
