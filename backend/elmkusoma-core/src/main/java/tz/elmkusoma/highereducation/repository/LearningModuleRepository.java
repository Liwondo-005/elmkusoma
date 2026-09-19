package tz.elmkusoma.highereducation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tz.elmkusoma.highereducation.domain.LearningModule;

import java.util.List;
import java.util.UUID;

public interface LearningModuleRepository extends JpaRepository<LearningModule, UUID> {
    List<LearningModule> findByStudentIdOrderByCreatedAtDesc(UUID studentId);
    List<LearningModule> findByStudentIdAndCourseId(UUID studentId, UUID courseId);
    List<LearningModule> findByInstitutionId(UUID institutionId);
}
