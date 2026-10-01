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
public class DistrictSummary {
    private UUID id;
    private String name;
    private String code;
    private Boolean isActive;
    private long institutionCount;
    private long schoolCount;
    private long teacherCount;
    private long learnerCount;
    private Double attendanceRate;
    private Double averagePerformance;
}
