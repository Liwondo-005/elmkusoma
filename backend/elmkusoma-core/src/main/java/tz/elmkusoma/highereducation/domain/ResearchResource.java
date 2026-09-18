package tz.elmkusoma.highereducation.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "research_resources")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ResearchResource extends BaseEntity {

    @Column(name = "research_project_id", nullable = false)
    private UUID researchProjectId;

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "resource_type", length = 50)
    private String resourceType;

    @Column(name = "file_url", length = 500)
    private String fileUrl;

    @Column(name = "citation", columnDefinition = "TEXT")
    private String citation;

    @Builder.Default
    @Column(name = "sort_order")
    private Integer sortOrder = 0;
}
