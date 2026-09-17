package tz.elmkusoma.parent.service;

import tz.elmkusoma.parent.dto.response.*;

import java.util.List;
import java.util.UUID;

public interface ParentDashboardService {

    FamilyOverviewResponse getFamilyOverview(UUID userId);

    List<ChildOverviewResponse> getMyChildren(UUID userId);

    ChildOverviewResponse getChildOverview(UUID userId, UUID studentId);

    ParentAttendanceResponse getChildAttendance(UUID userId, UUID studentId);

    ParentAssignmentResponse getChildAssignments(UUID userId, UUID studentId);

    ParentResultResponse getChildResults(UUID userId, UUID studentId);

    ParentAssessmentResponse getChildAssessments(UUID userId, UUID studentId);

    ParentLiveClassResponse getChildLiveClasses(UUID userId, UUID studentId);

    ParentNotificationResponse getChildNotifications(UUID userId, UUID studentId);

    ParentLearningProgressResponse getChildLearningProgress(UUID userId, UUID studentId);

    ParentAnnouncementResponse getChildAnnouncements(UUID userId, UUID studentId);

    ParentAllNotificationsResponse getAllChildrenNotifications(UUID userId);

    ParentUpcomingResponse getUpcomingActivities(UUID userId);
}
