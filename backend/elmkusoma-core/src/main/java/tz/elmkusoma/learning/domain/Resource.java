package tz.elmkusoma.learning.domain;

import jakarta.persistence.*;
import lombok.*;
import tz.elmkusoma.common.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "resources")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Resource extends BaseEntity {

    @Column(name = "subject_id")
    private UUID subjectId;

    @Column(name = "class_group_id")
    private UUID classGroupId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "file_url", nullable = false)
    private String fileUrl;

    @Column(name = "resource_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ResourceType resourceType;

    public enum ResourceType {
        DOCUMENT,
        VIDEO,
        IMAGE,
        AUDIO,
        LINK,
        OTHER
    }
}
