package tz.elmkusoma.student.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tz.elmkusoma.student.domain.StudentStatus;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentResponse {

    private UUID id;
    private UUID userId;
    private String admissionNumber;
    private StudentStatus status;
    private String firstName;
    private String middleName;
    private String lastName;
    private String email;
    private String phone;
    private LocalDate dateOfBirth;
    private String gender;
    private String address;
    private String city;
    private String region;
    private String guardianName;
    private String guardianPhone;
    private String guardianRelationship;
    private LocalDate enrollmentDate;
    private UUID institutionId;
}
