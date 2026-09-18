package tz.elmkusoma.highereducation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentCompetencyDTO {

    private UUID competencyId;
    private String name;
    private String code;
    private String description;
    private String competencyType;
    private UUID subjectId;
    private Integer sortOrder;
    private String status;
    private String evidence;
    private UUID assessedBy;
    private String assessmentDate;
    private String lastPracticeDate;
    private String notes;
}
