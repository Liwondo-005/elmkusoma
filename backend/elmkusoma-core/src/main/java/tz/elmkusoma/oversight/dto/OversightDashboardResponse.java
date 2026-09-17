package tz.elmkusoma.oversight.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OversightDashboardResponse {
    private Long totalInstitutions;
    private Long totalTeachers;
    private Long totalStudents;
    private Long totalUsers;
    private Long activeLiveClasses;
    private Long totalLessons;
    private Long totalClasses;
    private Long totalRegions;
    private Long totalDistricts;
    private Double attendanceRate;
    private Double averagePerformance;
    private Double curriculumProgress;
    private Long alertsCount;
    private JurisdictionSummary jurisdictionSummary;
    private java.util.List<TopRegionStats> topRegions;
    private java.util.List<RecentActivity> recentActivities;
    private java.util.List<OversightAlert> recentAlerts;
}