package tz.elmkusoma.nursery.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NurseryMilestoneResponse {
    private UUID id;
    private UUID studentId;
    private String studentName;
    private String category;
    private String milestoneName;
    private String description;
    private Integer expectedAgeMonths;
    private LocalDate achievedDate;
    private String status;
    private String observedByName;
    private String evidenceNotes;
}