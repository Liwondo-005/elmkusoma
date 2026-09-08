package tz.elmkusoma.attendance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.attendance.domain.AttendanceRecord;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, UUID> {

    List<AttendanceRecord> findByStudentIdAndIsDeletedFalse(UUID studentId);

    List<AttendanceRecord> findByClassGroupIdAndAttendanceDateAndIsDeletedFalse(UUID classGroupId, LocalDate attendanceDate);

    List<AttendanceRecord> findByInstitutionIdAndIsDeletedFalse(UUID institutionId);

    @Query("SELECT ar FROM AttendanceRecord ar WHERE ar.classGroupId = :classGroupId AND ar.attendanceDate BETWEEN :startDate AND :endDate AND ar.isDeleted = false")
    List<AttendanceRecord> findByClassGroupAndDateRange(
            @Param("classGroupId") UUID classGroupId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT ar FROM AttendanceRecord ar WHERE ar.studentId = :studentId AND ar.attendanceDate BETWEEN :startDate AND :endDate AND ar.isDeleted = false")
    List<AttendanceRecord> findByStudentAndDateRange(
            @Param("studentId") UUID studentId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT ar FROM AttendanceRecord ar WHERE ar.studentId = :studentId AND ar.status = :status AND ar.attendanceDate BETWEEN :startDate AND :endDate AND ar.isDeleted = false")
    List<AttendanceRecord> findByStudentAndStatusAndDateRange(
            @Param("studentId") UUID studentId,
            @Param("status") AttendanceRecord.AttendanceStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(ar) FROM AttendanceRecord ar WHERE ar.classGroupId = :classGroupId AND ar.attendanceDate = :date AND ar.status = 'PRESENT' AND ar.isDeleted = false")
    long countPresentByClassAndDate(@Param("classGroupId") UUID classGroupId, @Param("date") LocalDate date);
}