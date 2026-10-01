package tz.elmkusoma.administration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import tz.elmkusoma.oversight.dto.JurisdictionSummary;
import tz.elmkusoma.oversight.dto.OversightAlert;


import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegionalDashboardResponse {
    private JurisdictionSummary jurisdictionSummary;
    private long totalInstitutions;
    private long totalSchools;
    private long totalDistricts;
    private long totalTeachers;
    private long totalLearners;
    private long totalClasses;
    private long totalLessons;
    private long totalCourses;
    private long activeLiveClasses;
    private Double attendanceRate;
    private Double averagePerformance;
    private Double curriculumProgress;
    private long alertsCount;
    private long pendingVerifications;
    private long dataQualityIssues;
    private List<DistrictSummary> topDistricts;
    private List<OversightAlert> recentAlerts;
    private List<QuickActionResponse> quickActions;
    private LocalDateTime lastUpdated;
}
