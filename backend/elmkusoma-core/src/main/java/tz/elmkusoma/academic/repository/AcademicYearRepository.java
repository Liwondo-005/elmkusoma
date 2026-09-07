package tz.elmkusoma.academic.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.academic.domain.AcademicYear;
import tz.elmkusoma.academic.domain.EducationLevel;

import java.util.List;
import java.util.UUID;

@Repository
public interface AcademicYearRepository extends JpaRepository<AcademicYear, UUID> {

    List<AcademicYear> findByInstitutionIdAndIsDeletedFalse(UUID institutionId);

    List<AcademicYear> findByInstitutionIdAndEducationLevelAndIsDeletedFalse(UUID institutionId, EducationLevel level);

    List<AcademicYear> findByInstitutionIdAndIsCurrentTrueAndIsDeletedFalse(UUID institutionId);
}
