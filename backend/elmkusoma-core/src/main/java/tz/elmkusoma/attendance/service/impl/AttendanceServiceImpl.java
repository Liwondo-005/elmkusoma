package tz.elmkusoma.attendance.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.academic.domain.ClassGroup;
import tz.elmkusoma.academic.repository.ClassGroupRepository;
import tz.elmkusoma.attendance.domain.AttendanceRecord;
import tz.elmkusoma.attendance.domain.AttendanceSummary;
import tz.elmkusoma.attendance.dto.request.BulkMarkAttendanceRequest;
import tz.elmkusoma.attendance.dto.request.MarkAttendanceRequest;
import tz.elmkusoma.attendance.dto.response.AttendanceRecordResponse;
import tz.elmkusoma.attendance.dto.response.AttendanceSummaryResponse;
import tz.elmkusoma.attendance.repository.AttendanceRecordRepository;
import tz.elmkusoma.attendance.repository.AttendanceSummaryRepository;
import tz.elmkusoma.attendance.service.AttendanceService;
import tz.elmkusoma.common.OwnershipGuard;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.student.domain.Student;
import tz.elmkusoma.student.repository.StudentRepository;

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
    private final OwnershipGuard ownershipGuard;
    private final ClassGroupRepository classGroupRepository;
    private final StudentRepository studentRepository;

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
    public List<AttendanceRecordResponse> getByClassAndDate(UUID institutionId, UUID classGroupId, LocalDate date) {
        verifyClassGroupInScope(institutionId, classGroupId);
        return attendanceRecordRepository.findByClassGroupIdAndAttendanceDateAndIsDeletedFalse(classGroupId, date)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceRecordResponse> getByStudentAndDateRange(UUID institutionId, UUID studentId, LocalDate startDate, LocalDate endDate) {
        verifyStudentInScope(institutionId, studentId);
        return attendanceRecordRepository.findByStudentAndDateRange(studentId, startDate, endDate)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceSummaryResponse getSummary(UUID institutionId, UUID studentId, UUID termId) {
        verifyStudentInScope(institutionId, studentId);
        return attendanceSummaryRepository.findByStudentIdAndTermIdAndIsDeletedFalse(studentId, termId)
                .map(this::mapToSummaryResponse)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceSummaryResponse> getByClassAndTerm(UUID institutionId, UUID classGroupId, UUID termId) {
        verifyClassGroupInScope(institutionId, classGroupId);
        return attendanceSummaryRepository.findByClassGroupIdAndTermIdAndIsDeletedFalse(classGroupId, termId)
                .stream()
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

    /**
     * Tenant check for class-keyed attendance reads: the class group owns the
     * tenant boundary (its {@code institution_id} column is NOT NULL and
     * server-set). Unknown class group -&gt; 404; another tenant's class
     * group -&gt; 403.
     */
    private void verifyClassGroupInScope(UUID institutionId, UUID classGroupId) {
        ClassGroup classGroup = classGroupRepository.findById(classGroupId)
                .orElseThrow(() -> new ResourceNotFoundException("Class group not found"));
        ownershipGuard.verifyInstitution(classGroup.getInstitutionId(), institutionId);
    }

    /**
     * Tenant check for student-keyed attendance reads: the student owns the
     * tenant boundary. Unknown student -&gt; 404; another tenant's student
     * -&gt; 403.
     */
    private void verifyStudentInScope(UUID institutionId, UUID studentId) {
        Student student = studentRepository.findByIdAndIsDeletedFalse(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        ownershipGuard.verifyInstitution(student.getInstitutionId(), institutionId);
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