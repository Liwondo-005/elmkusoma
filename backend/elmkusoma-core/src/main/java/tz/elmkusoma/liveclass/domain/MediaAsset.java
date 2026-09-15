package tz.elmkusoma.liveclass.domain;

import jakarta.persistence.*;
import lombok.*;
import tz.elmkusoma.common.BaseEntity;

@Entity
@Table(name = "media_assets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaAsset extends BaseEntity {

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "media_type", nullable = false)
    private String mediaType;

    @Column(name = "file_url", length = 1000)
    private String fileUrl;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "duration_seconds")
    private Long durationSeconds;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "status")
    @Builder.Default
    private String status = "READY";

    @Column(name = "visibility")
    @Builder.Default
    private String visibility = "INSTITUTION";

    @Column(name = "source_type")
    private String sourceType;

    @Column(name = "source_id")
    private java.util.UUID sourceId;

    @Column(name = "teacher_id")
    private java.util.UUID teacherId;

    @Column(name = "course_id")
    private java.util.UUID courseId;

    @Column(name = "subject_id")
    private java.util.UUID subjectId;

    @Column(name = "tags", length = 500)
    private String tags;
}
