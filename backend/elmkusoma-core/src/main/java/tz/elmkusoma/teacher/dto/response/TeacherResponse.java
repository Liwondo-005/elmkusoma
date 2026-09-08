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
    private LocalDateTime createdAt;
}
