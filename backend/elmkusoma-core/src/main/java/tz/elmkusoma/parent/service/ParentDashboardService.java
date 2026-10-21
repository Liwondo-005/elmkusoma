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

    /**
     * Financial notifications - payments, receipts and refunds - for every verified child of this
     * parent, in the same item shape the notifications endpoint already returns.
     *
     * <p>Only ever derived from this parent's own records, so one family's money can never surface
     * in another's inbox. Empty for a caller with no parent profile rather than an error: such a
     * caller has no parent financial records, and the endpoint must keep working for them.
     * Read-only by construction - a dashboard GET must never send, persist or otherwise act on a
     * payment.
     */
    List<ParentNotificationResponse.NotificationItem> getFinancialNotifications(UUID userId);

    ParentUpcomingResponse getUpcomingActivities(UUID userId);
}
