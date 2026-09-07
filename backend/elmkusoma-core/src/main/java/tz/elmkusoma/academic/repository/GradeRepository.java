package tz.elmkusoma.academic.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.academic.domain.EducationLevel;
import tz.elmkusoma.academic.domain.Grade;

import java.util.List;
import java.util.UUID;

@Repository
public interface GradeRepository extends JpaRepository<Grade, UUID> {

    List<Grade> findByInstitutionIdAndIsDeletedFalse(UUID institutionId);

    List<Grade> findByInstitutionIdAndEducationLevelAndIsDeletedFalse(UUID institutionId, EducationLevel level);
}
