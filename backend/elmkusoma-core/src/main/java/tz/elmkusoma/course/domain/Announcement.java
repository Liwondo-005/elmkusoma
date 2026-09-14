package tz.elmkusoma.course.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

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

    public enum Priority {
        LOW, NORMAL, HIGH, URGENT
    }
}
