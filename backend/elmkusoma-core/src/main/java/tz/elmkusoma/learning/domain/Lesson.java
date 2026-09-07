package tz.elmkusoma.learning.domain;

import jakarta.persistence.*;
import lombok.*;
import tz.elmkusoma.common.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "lessons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lesson extends BaseEntity {

    @Column(name = "subject_id", nullable = false)
    private UUID subjectId;

    @Column(name = "class_group_id", nullable = false)
    private UUID classGroupId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "content_text", columnDefinition = "TEXT")
    private String contentText;

    @Column(name = "video_url")
    private String videoUrl;

    @Column(name = "file_attachments")
    private String fileAttachments;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "is_published", nullable = false)
    private Boolean isPublished = false;
}
