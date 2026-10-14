package tz.elmkusoma.attendance.service;

import tz.elmkusoma.attendance.dto.request.BulkMarkAttendanceRequest;
import tz.elmkusoma.attendance.dto.request.MarkAttendanceRequest;
import tz.elmkusoma.attendance.dto.response.AttendanceRecordResponse;
import tz.elmkusoma.attendance.dto.response.AttendanceSummaryResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AttendanceService {

    AttendanceRecordResponse markAttendance(UUID institutionId, UUID markedBy, MarkAttendanceRequest request);

    List<AttendanceRecordResponse> getByClassAndDate(UUID institutionId, UUID classGroupId, LocalDate date);

    List<AttendanceRecordResponse> getByStudentAndDateRange(UUID institutionId, UUID studentId, LocalDate startDate, LocalDate endDate);

    AttendanceSummaryResponse getSummary(UUID institutionId, UUID studentId, UUID termId);

    List<AttendanceSummaryResponse> getByClassAndTerm(UUID institutionId, UUID classGroupId, UUID termId);

    void markBulkAttendance(UUID institutionId, UUID markedBy, BulkMarkAttendanceRequest request);

    void updateSummary(UUID studentId, UUID termId, UUID classGroupId, UUID academicYearId, UUID institutionId);
}