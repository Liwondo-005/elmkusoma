package tz.elmkusoma.highereducation.dto;

import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class ProfessionalDevelopmentGoalDTO {
    private UUID id;
    private UUID studentId;
    private String title;
    private String description;
    private String goalType;
    private LocalDate targetDate;
    private LocalDate completedDate;
    private String status;
    private Integer progressPercent;
    private String evidenceUrl;
    private UUID institutionId;
    private String notes;
    private String category;
}
