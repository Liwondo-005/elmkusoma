package tz.elmkusoma.assessment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.assessment.domain.Assessment;

import java.util.List;
import java.util.UUID;

@Repository
public interface AssessmentRepository extends JpaRepository<Assessment, UUID> {

    List<Assessment> findByClassGroupIdAndIsDeletedFalse(UUID classGroupId);

    List<Assessment> findBySubjectIdAndIsDeletedFalse(UUID subjectId);

    List<Assessment> findByInstitutionIdAndIsDeletedFalse(UUID institutionId);

    @Query("SELECT COUNT(a) FROM Assessment a WHERE a.institutionId IN :institutionIds AND a.isDeleted = false")
    long countByInstitutionIdsAndIsDeletedFalse(@Param("institutionIds") List<UUID> institutionIds);

    @Query("SELECT COUNT(a) FROM Assessment a WHERE a.institutionId IN :institutionIds AND a.status = :status AND a.isDeleted = false")
    long countByInstitutionIdsAndStatusAndIsDeletedFalse(@Param("institutionIds") List<UUID> institutionIds, @Param("status") String status);

    @Query("SELECT a FROM Assessment a WHERE a.subjectId = :subjectId AND a.institutionId IN :institutionIds AND a.isDeleted = false")
    List<Assessment> findBySubjectIdAndInstitutionIdsAndIsDeletedFalse(
            @Param("subjectId") UUID subjectId,
            @Param("institutionIds") List<UUID> institutionIds);

    @Query(value = "SELECT a FROM Assessment a WHERE a.institutionId IN :institutionIds AND a.isDeleted = false ORDER BY a.scheduledAt DESC")
    List<Assessment> findRecentByInstitutionIds(
            @Param("institutionIds") List<UUID> institutionIds);

    @Query("SELECT COUNT(a) FROM Assessment a WHERE a.subjectId = :subjectId AND a.institutionId IN :institutionIds AND a.isDeleted = false")
    long countBySubjectIdAndInstitutionIdsAndIsDeletedFalse(
            @Param("subjectId") UUID subjectId,
            @Param("institutionIds") List<UUID> institutionIds);
}