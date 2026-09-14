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
public class CourseModuleResponse {

    private UUID id;
    private UUID courseId;
    private String title;
    private String description;
    private Integer sortOrder;
    private Long lessonCount;
    private LocalDateTime createdAt;
}
