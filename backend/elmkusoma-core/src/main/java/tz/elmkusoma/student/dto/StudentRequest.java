package tz.elmkusoma.student.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tz.elmkusoma.student.domain.StudentStatus;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentRequest {

    @NotNull(message = "Institution ID is required")
    private UUID institutionId;

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotBlank(message = "First name is required")
    @Size(max = 255, message = "First name must not exceed 255 characters")
    private String firstName;

    @Size(max = 255, message = "Middle name must not exceed 255 characters")
    private String middleName;

    @NotBlank(message = "Last name is required")
    @Size(max = 255, message = "Last name must not exceed 255 characters")
    private String lastName;

    @Email(message = "Email must be valid")
    @NotBlank(message = "Email is required")
    private String email;

    @Size(max = 255, message = "Phone must not exceed 255 characters")
    private String phone;

    private LocalDate dateOfBirth;

    private String gender;

    private String address;

    private String city;

    private String region;

    private String nationalId;

    private String bloodGroup;

    private String medicalNotes;

    @Size(max = 255, message = "Guardian name must not exceed 255 characters")
    private String guardianName;

    @Size(max = 255, message = "Guardian phone must not exceed 255 characters")
    private String guardianPhone;

    @Size(max = 255, message = "Guardian relationship must not exceed 255 characters")
    private String guardianRelationship;

    private StudentStatus status;
}
