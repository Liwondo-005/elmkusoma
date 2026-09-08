package tz.elmkusoma.attendance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.attendance.domain.AttendanceSummary;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttendanceSummaryRepository extends JpaRepository<AttendanceSummary, UUID> {

    List<AttendanceSummary> findByStudentIdAndIsDeletedFalse(UUID studentId);

    Optional<AttendanceSummary> findByStudentIdAndTermIdAndIsDeletedFalse(UUID studentId, UUID termId);

    List<AttendanceSummary> findByClassGroupIdAndTermIdAndIsDeletedFalse(UUID classGroupId, UUID termId);

    List<AttendanceSummary> findByInstitutionIdAndIsDeletedFalse(UUID institutionId);

    @Query("SELECT asum FROM AttendanceSummary asum WHERE asum.termId = :termId AND asum.attendancePercentage < :minPercentage AND asum.isDeleted = false")
    List<AttendanceSummary> findStudentsWithLowAttendance(
            @Param("termId") UUID termId,
            @Param("minPercentage") double minPercentage);
}