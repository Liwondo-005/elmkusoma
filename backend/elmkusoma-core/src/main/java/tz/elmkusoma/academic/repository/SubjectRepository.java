package tz.elmkusoma.academic.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.academic.domain.EducationLevel;
import tz.elmkusoma.academic.domain.Subject;

import java.util.List;
import java.util.UUID;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, UUID> {

    List<Subject> findByInstitutionIdAndIsDeletedFalse(UUID institutionId);

    List<Subject> findByInstitutionIdAndEducationLevelAndIsDeletedFalse(UUID institutionId, EducationLevel level);

    @Query("SELECT s FROM Subject s WHERE s.institutionId IN :institutionIds AND s.isDeleted = false")
    List<Subject> findByInstitutionIdsAndIsDeletedFalse(@Param("institutionIds") List<UUID> institutionIds);
}