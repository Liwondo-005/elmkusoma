package tz.elmkusoma.highereducation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompetencyRecordDTO {

    private UUID id;
    private UUID studentId;
    private UUID competencyId;
    private String competencyName;
    private String competencyCode;
    private String competencyType;
    private String status;
    private String evidence;
    private UUID assessedBy;
    private LocalDate assessmentDate;
    private LocalDate lastPracticeDate;
    private String notes;
}
