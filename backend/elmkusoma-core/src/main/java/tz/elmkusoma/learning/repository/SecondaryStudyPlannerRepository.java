package tz.elmkusoma.learning.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.learning.domain.SecondaryStudyPlanner;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface SecondaryStudyPlannerRepository extends JpaRepository<SecondaryStudyPlanner, UUID> {
    List<SecondaryStudyPlanner> findByStudentIdAndIsDeletedFalse(UUID studentId);
    List<SecondaryStudyPlanner> findByStudentIdAndPlannedDateAndIsDeletedFalse(UUID studentId, LocalDate plannedDate);
    List<SecondaryStudyPlanner> findByStudentIdAndStatusAndIsDeletedFalse(UUID studentId, SecondaryStudyPlanner.PlannerStatus status);
    List<SecondaryStudyPlanner> findByStudentIdAndPlannedDateBetweenAndIsDeletedFalse(UUID studentId, LocalDate startDate, LocalDate endDate);
}
