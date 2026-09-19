package tz.elmkusoma.administration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformDashboardResponse {
    private long totalUsers;
    private long totalStudents;
    private long totalTeachers;
    private long totalParents;
    private long totalInstitutions;
    private long totalLiveClasses;
    private long activeLiveClasses;
    private long totalPayments;
    private long totalCertificates;
    private long unresolvedSecurityEvents;
}
