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
public class TeacherStudentResponse {

    private UUID studentId;
    private String fullName;
    private String email;
    private String admissionNumber;
    private String className;
    private String subjectName;
    private String gender;
    private String status;
    private UUID classGroupId;
}
