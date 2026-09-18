package tz.elmkusoma.highereducation.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "portfolio_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PortfolioItem extends BaseEntity {

    @Column(name = "portfolio_id", nullable = false)
    private UUID portfolioId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false)
    private PortfolioItemType itemType;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "competency_id")
    private UUID competencyId;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "date_obtained")
    private LocalDate dateObtained;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Builder.Default
    @Column(name = "is_visible", nullable = false)
    private Boolean isVisible = true;
}
