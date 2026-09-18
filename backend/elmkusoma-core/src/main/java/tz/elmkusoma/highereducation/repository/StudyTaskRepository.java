package tz.elmkusoma.highereducation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.highereducation.domain.StudyTask;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface StudyTaskRepository extends JpaRepository<StudyTask, UUID> {

    List<StudyTask> findByInstitutionIdAndIsDeletedFalse(UUID institutionId);

    List<StudyTask> findByStudentIdAndIsDeletedFalse(UUID studentId);

    List<StudyTask> findByStudentIdAndScheduledDateAndIsDeletedFalse(UUID studentId, LocalDate scheduledDate);

    List<StudyTask> findByStudentIdAndIsCompletedAndIsDeletedFalse(UUID studentId, Boolean isCompleted);

    List<StudyTask> findByStudentIdAndScheduledDateBetweenAndIsDeletedFalse(UUID studentId, LocalDate startDate, LocalDate endDate);
}
