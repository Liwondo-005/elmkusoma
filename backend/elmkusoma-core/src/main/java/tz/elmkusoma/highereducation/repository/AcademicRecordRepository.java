package tz.elmkusoma.highereducation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tz.elmkusoma.highereducation.domain.AcademicRecord;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AcademicRecordRepository extends JpaRepository<AcademicRecord, UUID> {
    Optional<AcademicRecord> findByStudentIdAndSemesterAndAcademicYearAndIsDeletedFalse(UUID studentId, String semester, String academicYear);
    List<AcademicRecord> findByStudentIdAndIsDeletedFalse(UUID studentId);
    Optional<AcademicRecord> findTopByStudentIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID studentId);
}
