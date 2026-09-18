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
public class CompetencyAssessmentDTO {

    private UUID id;
    private UUID competencyId;
    private UUID assessmentId;
    private Integer weight;
}
