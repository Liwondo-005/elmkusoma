package tz.elmkusoma.attendance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.attendance.domain.BulkAttendanceSession;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface BulkAttendanceSessionRepository extends JpaRepository<BulkAttendanceSession, UUID> {

    List<BulkAttendanceSession> findByClassGroupIdAndAttendanceDateAndIsDeletedFalse(UUID classGroupId, LocalDate attendanceDate);

    List<BulkAttendanceSession> findByInstitutionIdAndIsDeletedFalse(UUID institutionId);

    @Query("SELECT bas FROM BulkAttendanceSession bas WHERE bas.classGroupId = :classGroupId AND bas.attendanceDate = :date AND bas.status = 'IN_PROGRESS' AND bas.isDeleted = false")
    List<BulkAttendanceSession> findActiveSessionsByClassAndDate(
            @Param("classGroupId") UUID classGroupId,
            @Param("date") LocalDate date);
}