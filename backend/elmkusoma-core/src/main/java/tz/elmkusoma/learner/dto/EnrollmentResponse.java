package tz.elmkusoma.learner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentResponse {

    private UUID id;
    private UUID courseId;
    private String courseTitle;
    private String courseDescription;
    private String courseThumbnailUrl;
    private String courseLevel;
    private String courseCategory;
    private LocalDateTime enrolledAt;
    private LocalDateTime completedAt;
    private Double progressPercentage;
}
