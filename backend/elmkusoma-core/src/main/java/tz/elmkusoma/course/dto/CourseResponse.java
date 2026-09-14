package tz.elmkusoma.course.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseResponse {

    private UUID id;
    private UUID institutionId;
    private UUID subjectId;
    private String subjectName;
    private String title;
    private String description;
    private String thumbnailUrl;
    private String level;
    private String category;
    private Boolean isPublished;
    private Boolean isFeatured;
    private String createdByName;
    private Long moduleCount;
    private Long lessonCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
