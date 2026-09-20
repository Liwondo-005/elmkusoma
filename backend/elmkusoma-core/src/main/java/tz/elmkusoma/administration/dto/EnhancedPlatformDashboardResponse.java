package tz.elmkusoma.administration.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class EnhancedPlatformDashboardResponse {
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
    private long totalProviders;
    private long totalEvents;
    private long totalEntitlements;
    private long openIncidents;
    private long pendingVerifications;
    private long activeServices;
    private long totalNotifications;
    private long activeDelegations;
}
