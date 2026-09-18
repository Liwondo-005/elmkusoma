package tz.elmkusoma.highereducation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tz.elmkusoma.highereducation.domain.ThesisStatus;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThesisDTO {

    private UUID id;

    @NotNull(message = "Student ID is required")
    private UUID studentId;

    @NotBlank(message = "Thesis title is required")
    private String title;

    private UUID researchProjectId;

    private UUID supervisorId;

    @Builder.Default
    private ThesisStatus status = ThesisStatus.NOT_STARTED;

    private UUID programmeId;

    private LocalDate submissionDate;

    private LocalDate defenseDate;

    private String finalGrade;

    private String abstractText;

    private Integer wordCount;

    private UUID institutionId;
}
