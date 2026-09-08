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
public class SubmissionResponse {

    private UUID id;
    private UUID assignmentId;
    private UUID studentId;
    private String fileUrl;
    private LocalDateTime submittedAt;
    private Integer grade;
    private String feedback;
    private LocalDateTime gradedAt;
    private UUID gradedBy;
    private LocalDateTime createdAt;
}
