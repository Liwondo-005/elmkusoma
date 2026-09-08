package tz.elmkusoma.teacher.dto.response;

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
public class TeacherAssignmentResponse {

    private UUID id;
    private UUID teacherId;
    private UUID classGroupId;
    private UUID subjectId;
    private String academicYear;
    private LocalDateTime createdAt;
}
