package tz.elmkusoma.highereducation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tz.elmkusoma.highereducation.domain.DemonstrationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemonstrationDTO {

    private UUID id;

    @NotNull(message = "Student ID is required")
    private UUID studentId;

    private UUID competencyId;

    private UUID projectId;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private String mediaUrls;

    @Builder.Default
    private DemonstrationStatus status = DemonstrationStatus.DRAFT;

    private UUID reviewerId;

    private String reviewNotes;

    private Integer score;

    private LocalDateTime reviewedAt;

    private LocalDateTime submittedAt;

    private UUID institutionId;
}
