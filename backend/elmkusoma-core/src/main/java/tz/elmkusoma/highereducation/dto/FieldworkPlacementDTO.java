package tz.elmkusoma.highereducation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tz.elmkusoma.highereducation.domain.PlacementStatus;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldworkPlacementDTO {

    private UUID id;

    @NotNull(message = "Student ID is required")
    private UUID studentId;

    private UUID programmeId;

    @NotBlank(message = "Organisation name is required")
    private String organisationName;

    @NotBlank(message = "Placement title is required")
    private String placementTitle;

    private String supervisorName;

    private String supervisorEmail;

    private String supervisorPhone;

    private UUID institutionSupervisorId;

    private LocalDate startDate;

    private LocalDate endDate;

    @Builder.Default
    private PlacementStatus status = PlacementStatus.PLANNING;

    private Integer totalHoursRequired;

    @Builder.Default
    private Integer totalHoursCompleted = 0;

    private String objectives;

    private String remarks;

    private UUID institutionId;
}
