package tz.elmkusoma.highereducation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.highereducation.domain.Project;
import tz.elmkusoma.highereducation.domain.ProjectStatus;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {

    List<Project> findByInstitutionIdAndIsDeletedFalse(UUID institutionId);

    List<Project> findByStudentIdAndIsDeletedFalse(UUID studentId);

    List<Project> findBySubjectIdAndIsDeletedFalse(UUID subjectId);

    List<Project> findByStatusAndIsDeletedFalse(ProjectStatus status);

    List<Project> findByStudentIdAndInstitutionIdAndIsDeletedFalse(UUID studentId, UUID institutionId);

    List<Project> findByInstructorIdAndIsDeletedFalse(UUID instructorId);
}
