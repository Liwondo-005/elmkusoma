package tz.elmkusoma.oversight.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstitutionDetailResponse {
    private UUID id;
    private String name;
    private String code;
    private String type;
    private Boolean isActive;
    private Long teacherCount;
    private Long studentCount;
    private Long classCount;
    private Long lessonCount;
    private Long activeLiveClasses;
    private Double attendanceRate;
    private Double averagePerformance;
    private Double curriculumProgress;
    private String address;
    private String city;
    private String regionName;
    private String districtName;
}