package tz.elmkusoma.academic.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.academic.domain.EducationLevel;
import tz.elmkusoma.academic.domain.Subject;

import java.util.List;
import java.util.UUID;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, UUID> {

    List<Subject> findByInstitutionIdAndIsDeletedFalse(UUID institutionId);

    List<Subject> findByInstitutionIdAndEducationLevelAndIsDeletedFalse(UUID institutionId, EducationLevel level);
}
