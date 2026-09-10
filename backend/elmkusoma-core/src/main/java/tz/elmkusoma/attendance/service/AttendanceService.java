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

    List<AttendanceRecordResponse> getByClassAndDate(UUID classGroupId, LocalDate date, UUID institutionId);

    List<AttendanceRecordResponse> getByStudentAndDateRange(UUID studentId, LocalDate startDate, LocalDate endDate, UUID institutionId);

    AttendanceSummaryResponse getSummary(UUID studentId, UUID termId, UUID institutionId);

    List<AttendanceSummaryResponse> getByClassAndTerm(UUID classGroupId, UUID termId, UUID institutionId);

    void markBulkAttendance(UUID institutionId, UUID markedBy, BulkMarkAttendanceRequest request);

    void updateSummary(UUID studentId, UUID termId, UUID classGroupId, UUID academicYearId);
}