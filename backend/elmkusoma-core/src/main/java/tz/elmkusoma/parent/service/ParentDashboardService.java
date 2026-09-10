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
}
