package tz.elmkusoma.offering.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tz.elmkusoma.academic.domain.EducationLevel;
import tz.elmkusoma.learning.domain.Resource;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LearningOfferingResponse {

    private UUID id;
    private String title;
    private String description;
    private String thumbnailUrl;
    private EducationLevel educationLevel;
    private Resource.ResourceVisibility visibility;
    private String status;
    private UUID subjectId;
    private String subjectName;
    private UUID courseId;
    private String courseTitle;
    private UUID ownerId;
    private String ownerName;
    private UUID teacherId;
    private UUID institutionId;
    private LocalDateTime createdAt;
}
