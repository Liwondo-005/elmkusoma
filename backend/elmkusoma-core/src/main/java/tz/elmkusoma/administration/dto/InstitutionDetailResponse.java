package tz.elmkusoma.administration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstitutionDetailResponse {
    private UUID id;
    private String name;
    private String code;
    private String type;
    private String address;
    private String city;
    private String region;
    private String country;
    private String phone;
    private String email;
    private Boolean isActive;
    private long totalUsers;
    private long totalStudents;
    private long totalTeachers;
    private java.time.LocalDateTime createdAt;
}
