package tz.elmkusoma.teacher.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TeacherRequest {

    @NotBlank(message = "User ID is required")
    private String userId;

    private String employeeNumber;

    private String specialization;

    private LocalDate hireDate;

    private String bio;
}
