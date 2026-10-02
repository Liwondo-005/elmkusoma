package tz.elmkusoma.course.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "announcements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Announcement extends BaseEntity {

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "class_group_id")
    private UUID classGroupId;

    @Column(name = "subject_id")
    private UUID subjectId;

    @Column(name = "priority", nullable = false, length = 20)
    private String priority = "NORMAL";

    /**
     * Nationaladmin.md §23 — jurisdictional audience. NULL keeps the legacy
     * institution-scoped behaviour. Values: NATIONWIDE | REGION | DISTRICT.
     */
    @Column(name = "audience_type", length = 32)
    private String audienceType;

    @Column(name = "audience_region_id")
    private UUID audienceRegionId;

    @Column(name = "audience_district_id")
    private UUID audienceDistrictId;

    /** DRAFT | SCHEDULED | PUBLISHED */
    @Column(name = "status", length = 16)
    private String status = "PUBLISHED";

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    public enum Priority {
        LOW, NORMAL, HIGH, URGENT
    }
}
