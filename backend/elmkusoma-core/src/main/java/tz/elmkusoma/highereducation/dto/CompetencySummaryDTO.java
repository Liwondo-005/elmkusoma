package tz.elmkusoma.highereducation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompetencySummaryDTO {

    private long totalCompetencies;
    private long notStarted;
    private long learning;
    private long practicing;
    private long assessed;
    private long competent;
    private long needsPractice;
    private long completed;
}
