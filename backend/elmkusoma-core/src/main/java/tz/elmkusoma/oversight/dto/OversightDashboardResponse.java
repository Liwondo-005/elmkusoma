package tz.elmkusoma.oversight.dto;

import lombok.*;
import java.util.UUID;

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
    private Long totalRegions;
    private Long totalDistricts;
    private java.util.List<TopRegionStats> topRegions;
    private java.util.List<RecentActivity> recentActivities;
}
