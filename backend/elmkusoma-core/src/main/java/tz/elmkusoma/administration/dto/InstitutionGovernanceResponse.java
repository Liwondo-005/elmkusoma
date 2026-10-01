package tz.elmkusoma.administration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstitutionGovernanceResponse {
    private UUID id;
    private String name;
    private String code;
    private String type;
    private Boolean isActive;
    private String status;
    private UUID districtId;
    private String districtName;
    private UUID regionId;
    private String regionName;
    private long teacherCount;
    private long learnerCount;
    private long classCount;
    private long lessonCount;
    private long liveClassCount;
    private Double attendanceRate;
    private Double averagePerformance;
    private Double curriculumProgress;
    private String verificationStatus;
    private List<String> dataQualityIssues;
    private String contactEmail;
    private String contactPhone;
    private String address;
    private String city;
}
