package tz.elmkusoma.teacher.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherClassResponse {

    private UUID classGroupId;
    private String className;
    private String classSection;
    private UUID subjectId;
    private String subjectName;
    private String academicYear;
    private long enrolledStudents;
    private long totalAssignments;
    private long totalLessons;
}
