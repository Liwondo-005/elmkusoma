package tz.elmkusoma.academic.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.academic.domain.ClassGroup;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClassGroupRepository extends JpaRepository<ClassGroup, UUID> {

    List<ClassGroup> findByInstitutionIdAndIsDeletedFalse(UUID institutionId);

    List<ClassGroup> findByGradeIdAndIsDeletedFalse(UUID gradeId);

    List<ClassGroup> findByTermIdAndIsDeletedFalse(UUID termId);

    List<ClassGroup> findByGradeIdAndTermIdAndIsDeletedFalse(UUID gradeId, UUID termId);

    List<ClassGroup> findByAcademicYearIdAndIsDeletedFalse(UUID academicYearId);
}
