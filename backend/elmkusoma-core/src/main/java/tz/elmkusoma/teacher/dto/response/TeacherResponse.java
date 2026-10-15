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
public class TeacherResponse {

    private UUID id;
    private UUID userId;
    private String fullName;
    private String email;
    private String phone;
    private String employeeNumber;
    private String status;
    private String specialization;
    private LocalDate hireDate;
    private String bio;
    /**
     * The teacher's school affiliation: id from the persisted relationship
     * (teachers.institution_id, falling back to the user's organisation), name
     * resolved server-side from the institution record — never client-supplied.
     */
    private UUID institutionId;
    private String institutionName;
    private LocalDateTime createdAt;
}
