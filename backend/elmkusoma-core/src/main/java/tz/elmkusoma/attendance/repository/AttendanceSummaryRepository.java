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

    @Query("SELECT asum FROM AttendanceSummary asum WHERE asum.institutionId IN :institutionIds AND asum.termId = :termId AND asum.isDeleted = false")
    List<AttendanceSummary> findByInstitutionIdsAndTermIdAndIsDeletedFalse(
            @Param("institutionIds") List<UUID> institutionIds,
            @Param("termId") UUID termId);

    @Query("SELECT COUNT(asum) FROM AttendanceSummary asum WHERE asum.institutionId IN :institutionIds AND asum.termId = :termId AND asum.attendancePercentage < :threshold AND asum.isDeleted = false")
    long countByInstitutionIdsAndTermIdAndAttendanceBelow(
            @Param("institutionIds") List<UUID> institutionIds,
            @Param("termId") UUID termId,
            @Param("threshold") double threshold);

    @Query("SELECT COUNT(asum) FROM AttendanceSummary asum WHERE asum.institutionId IN :institutionIds AND asum.termId = :termId AND asum.daysLate > 0 AND asum.isDeleted = false")
    long countByInstitutionIdsAndTermIdAndLate(
            @Param("institutionIds") List<UUID> institutionIds,
            @Param("termId") UUID termId);
}