package tz.elmkusoma.highereducation.dto;

import lombok.*;
import java.util.UUID;

@Data
@Builder
public class LearningCollaborationDTO {
    private UUID id;
    private UUID studentId;
    private UUID peerStudentId;
    private String collaborationType;
    private String title;
    private String description;
    private UUID courseId;
    private UUID institutionId;
    private String status;
}
