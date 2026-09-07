package tz.elmkusoma.learning.dto.response;

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
public class AssignmentResponse {

    private UUID id;
    private UUID subjectId;
    private UUID classGroupId;
    private String title;
    private String description;
    private LocalDateTime dueDate;
    private Integer totalMarks;
    private String attachments;
    private LocalDateTime createdAt;
}
