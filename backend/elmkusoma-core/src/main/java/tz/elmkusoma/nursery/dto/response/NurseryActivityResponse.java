package tz.elmkusoma.nursery.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NurseryActivityResponse {
    private UUID id;
    private UUID classGroupId;
    private String className;
    private String activityName;
    private String activityType;
    private String description;
    private String instructions;
    private Integer durationMinutes;
    private Integer maxParticipants;
    private String materialsNeeded;
    private String learningObjectives;
    private String ageGroup;
    private LocalDate activityDate;
    private String status;
    private String conductedByName;
    private Integer participantCount;
    private LocalDateTime createdAt;
}