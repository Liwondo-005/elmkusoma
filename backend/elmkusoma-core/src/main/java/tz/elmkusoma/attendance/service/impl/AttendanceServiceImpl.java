package tz.elmkusoma.attendance.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.attendance.domain.AttendanceRecord;
import tz.elmkusoma.attendance.domain.AttendanceSummary;
import tz.elmkusoma.attendance.dto.request.BulkMarkAttendanceRequest;
import tz.elmkusoma.attendance.dto.request.MarkAttendanceRequest;
import tz.elmkusoma.attendance.dto.response.AttendanceRecordResponse;
import tz.elmkusoma.attendance.dto.response.AttendanceSummaryResponse;
import tz.elmkusoma.attendance.repository.AttendanceRecordRepository;
import tz.elmkusoma.attendance.repository.AttendanceSummaryRepository;
import tz.elmkusoma.attendance.service.AttendanceService;
import tz.elmkusoma.shared.security.OwnershipGuard;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final AttendanceSummaryRepository attendanceSummaryRepository;

    @Override
    public AttendanceRecordResponse markAttendance(UUID institutionId, UUID markedBy, MarkAttendanceRequest request) {
        AttendanceRecord record = AttendanceRecord.builder()
                .institutionId(institutionId)
                .studentId(request.getStudentId())
                .classGroupId(request.getClassGroupId())
                .attendanceDate(request.getAttendanceDate())
                .status(AttendanceRecord.AttendanceStatus.valueOf(request.getStatus()))
                .markedBy(markedBy)
                .remarks(request.getRemarks())
                .build();

        AttendanceRecord saved = attendanceRecordRepository.save(record);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceRecordResponse> getByClassAndDate(UUID classGroupId, LocalDate date, UUID institutionId) {
        return attendanceRecordRepository.findByClassGroupIdAndAttendanceDateAndIsDeletedFalse(classGroupId, date)
                .stream()
                .filter(r -> r.getInstitutionId().equals(institutionId))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceRecordResponse> getByStudentAndDateRange(UUID studentId, LocalDate startDate, LocalDate endDate, UUID institutionId) {
        return attendanceRecordRepository.findByStudentAndDateRange(studentId, startDate, endDate)
                .stream()
                .filter(r -> r.getInstitutionId().equals(institutionId))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceSummaryResponse getSummary(UUID studentId, UUID termId, UUID institutionId) {
        return attendanceSummaryRepository.findByStudentIdAndTermIdAndIsDeletedFalse(studentId, termId)
                .filter(s -> s.getInstitutionId().equals(institutionId))
                .map(this::mapToSummaryResponse)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceSummaryResponse> getByClassAndTerm(UUID classGroupId, UUID termId, UUID institutionId) {
        return attendanceSummaryRepository.findByClassGroupIdAndTermIdAndIsDeletedFalse(classGroupId, termId)
                .stream()
                .filter(s -> s.getInstitutionId().equals(institutionId))
                .map(this::mapToSummaryResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void markBulkAttendance(UUID institutionId, UUID markedBy, BulkMarkAttendanceRequest request) {
        for (BulkMarkAttendanceRequest.AttendanceEntry entry : request.getRecords()) {
            MarkAttendanceRequest markRequest = new MarkAttendanceRequest();
            markRequest.setStudentId(entry.getStudentId());
            markRequest.setClassGroupId(request.getClassGroupId());
            markRequest.setAttendanceDate(request.getAttendanceDate());
            markRequest.setStatus(entry.getStatus());
            markRequest.setRemarks(entry.getRemarks());

            markAttendance(institutionId, markedBy, markRequest);
        }
    }

    @Override
    public void updateSummary(UUID studentId, UUID termId, UUID classGroupId, UUID academicYearId, UUID institutionId) {
        AttendanceSummary summary = attendanceSummaryRepository
                .findByStudentIdAndTermIdAndIsDeletedFalse(studentId, termId)
                .orElse(AttendanceSummary.builder()
                        .institutionId(institutionId)
                        .studentId(studentId)
                        .classGroupId(classGroupId)
                        .academicYearId(academicYearId)
                        .termId(termId)
                        .build());

        // Get attendance records for the term
        LocalDate termStart = LocalDate.now().minusMonths(3); // Simplified
        LocalDate termEnd = LocalDate.now();
        
        List<AttendanceRecord> records = attendanceRecordRepository
                .findByStudentAndDateRange(studentId, termStart, termEnd);

        int totalDays = (int) ChronoUnit.DAYS.between(termStart, termEnd) + 1;
        int present = (int) records.stream()
                .filter(r -> r.getStatus() == AttendanceRecord.AttendanceStatus.PRESENT)
                .count();
        int absent = (int) records.stream()
                .filter(r -> r.getStatus() == AttendanceRecord.AttendanceStatus.ABSENT)
                .count();
        int late = (int) records.stream()
                .filter(r -> r.getStatus() == AttendanceRecord.AttendanceStatus.LATE)
                .count();
        int excused = (int) records.stream()
                .filter(r -> r.getStatus() == AttendanceRecord.AttendanceStatus.EXCUSED)
                .count();

        summary.setTotalSchoolDays(totalDays);
        summary.setDaysPresent(present);
        summary.setDaysAbsent(absent);
        summary.setDaysLate(late);
        summary.setDaysExcused(excused);

        if (totalDays > 0) {
            BigDecimal percentage = BigDecimal.valueOf(present)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalDays), 2, RoundingMode.HALF_UP);
            summary.setAttendancePercentage(percentage);
        }

        attendanceSummaryRepository.save(summary);
    }

    private AttendanceRecordResponse mapToResponse(AttendanceRecord record) {
        return AttendanceRecordResponse.builder()
                .id(record.getId())
                .studentId(record.getStudentId())
                .classGroupId(record.getClassGroupId())
                .attendanceDate(record.getAttendanceDate())
                .status(record.getStatus().name())
                .checkInTime(record.getCheckInTime())
                .checkOutTime(record.getCheckOutTime())
                .remarks(record.getRemarks())
                .createdAt(record.getCreatedAt())
                .build();
    }

    private AttendanceSummaryResponse mapToSummaryResponse(AttendanceSummary summary) {
        return AttendanceSummaryResponse.builder()
                .id(summary.getId())
                .studentId(summary.getStudentId())
                .classGroupId(summary.getClassGroupId())
                .academicYearId(summary.getAcademicYearId())
                .termId(summary.getTermId())
                .totalSchoolDays(summary.getTotalSchoolDays())
                .daysPresent(summary.getDaysPresent())
                .daysAbsent(summary.getDaysAbsent())
                .daysLate(summary.getDaysLate())
                .daysExcused(summary.getDaysExcused())
                .attendancePercentage(summary.getAttendancePercentage())
                .build();
    }
}