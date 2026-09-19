package tz.elmkusoma.highereducation.dto;

import lombok.*;
import java.util.UUID;

@Data
@Builder
public class DeepLearningContentDTO {
    private UUID id;
    private UUID studentId;
    private UUID courseId;
    private UUID moduleId;
    private String title;
    private String contentType;
    private String contentText;
    private String fileUrl;
    private String difficultyLevel;
    private String tags;
    private Boolean isCompleted;
    private Integer timeSpentMinutes;
    private UUID institutionId;
    private String notes;
}
