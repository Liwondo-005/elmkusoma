package tz.elmkusoma.nfe.attendance.service;

import tz.elmkusoma.common.PageResponse;
import tz.elmkusoma.nfe.attendance.dto.AttendanceRequest;
import tz.elmkusoma.nfe.attendance.dto.AttendanceResponse;

import java.util.List;
import java.util.UUID;

public interface NfeAttendanceService {

    AttendanceResponse recordAttendance(UUID institutionId, UUID providerId, AttendanceRequest request);

    AttendanceResponse getAttendance(UUID institutionId, UUID attendanceId);

    PageResponse<AttendanceResponse> listAttendanceBySession(UUID institutionId, UUID sessionId, int page, int size);

    List<AttendanceResponse> getAttendanceByLearner(UUID institutionId, UUID learnerId);

    List<AttendanceResponse> getAttendanceByProvider(UUID institutionId, UUID providerId);

    AttendanceResponse updateAttendance(UUID institutionId, UUID attendanceId, AttendanceRequest request);

    void deleteAttendance(UUID institutionId, UUID attendanceId);
}
