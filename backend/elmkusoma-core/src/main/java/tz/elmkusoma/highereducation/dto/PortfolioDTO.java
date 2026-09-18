package tz.elmkusoma.highereducation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tz.elmkusoma.highereducation.domain.Visibility;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioDTO {

    private UUID id;

    @NotNull(message = "Student ID is required")
    private UUID studentId;

    @NotBlank(message = "Portfolio title is required")
    private String title;

    @Builder.Default
    private Visibility visibility = Visibility.PRIVATE;

    @Builder.Default
    private Boolean isActive = true;

    private UUID institutionId;

    private List<PortfolioItemDTO> items;
}
