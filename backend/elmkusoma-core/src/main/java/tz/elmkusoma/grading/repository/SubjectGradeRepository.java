package tz.elmkusoma.grading.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.grading.domain.SubjectGrade;

import java.util.List;
import java.util.UUID;

@Repository
public interface SubjectGradeRepository extends JpaRepository<SubjectGrade, UUID> {

    List<SubjectGrade> findByReportCardIdAndIsDeletedFalse(UUID reportCardId);

    List<SubjectGrade> findBySubjectIdAndIsDeletedFalse(UUID subjectId);

    List<SubjectGrade> findByInstitutionIdAndIsDeletedFalse(UUID institutionId);
}