package tz.elmkusoma.highereducation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogbookEntryDTO {

    private UUID id;

    @NotNull(message = "Placement ID is required")
    private UUID placementId;

    @NotNull(message = "Entry date is required")
    private LocalDate entryDate;

    @NotBlank(message = "Activities description is required")
    private String activities;

    private Double hoursWorked;

    private String skillsUsed;

    private String challenges;

    private String learningOutcomes;

    private String supervisorComments;

    @Builder.Default
    private Boolean isApproved = false;

    private UUID approvedBy;

    private LocalDateTime approvedAt;
}
