package tz.elmkusoma.teacher.service;

import tz.elmkusoma.teacher.dto.response.TeacherAnalyticsResponse;

import java.util.UUID;

public interface TeacherAnalyticsService {
    TeacherAnalyticsResponse getAnalytics(UUID userId, UUID institutionId);
}
