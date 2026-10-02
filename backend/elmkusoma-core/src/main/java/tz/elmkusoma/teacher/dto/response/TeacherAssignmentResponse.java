package tz.elmkusoma.teacher.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
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
    /** ACTIVE / ENDED / CANCELLED — history rows are returned, never deleted. */
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private String classGroupName;
    private String subjectName;
    private LocalDateTime createdAt;
}
