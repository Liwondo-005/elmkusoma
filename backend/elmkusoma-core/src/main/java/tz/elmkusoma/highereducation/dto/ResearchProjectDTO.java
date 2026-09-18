package tz.elmkusoma.highereducation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tz.elmkusoma.highereducation.domain.ResearchStatus;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResearchProjectDTO {

    private UUID id;

    @NotNull(message = "Student ID is required")
    private UUID studentId;

    @NotBlank(message = "Research title is required")
    private String title;

    private String researchQuestion;

    private String objectives;

    private UUID supervisorId;

    @Builder.Default
    private ResearchStatus status = ResearchStatus.IDEA;

    private UUID programmeId;

    private UUID subjectId;

    private String methodology;

    private LocalDate startDate;

    private LocalDate dueDate;

    private LocalDate completedDate;

    private String abstractText;

    private String keywords;

    private UUID institutionId;
}
