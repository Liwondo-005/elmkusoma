package tz.elmkusoma.highereducation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tz.elmkusoma.highereducation.domain.PortfolioItemType;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioItemDTO {

    private UUID id;

    private UUID portfolioId;

    @NotBlank(message = "Item title is required")
    private String title;

    private String description;

    @NotNull(message = "Item type is required")
    private PortfolioItemType itemType;

    private String fileUrl;

    private UUID competencyId;

    private UUID projectId;

    private LocalDate dateObtained;

    private Integer sortOrder;

    @Builder.Default
    private Boolean isVisible = true;

    private UUID institutionId;
}
