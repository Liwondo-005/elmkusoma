package tz.elmkusoma.learning.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.learning.domain.Lesson;

import java.util.List;
import java.util.UUID;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, UUID> {

    List<Lesson> findBySubjectIdAndClassGroupIdAndIsDeletedFalseOrderBySortOrder(
            UUID subjectId, UUID classGroupId);

    List<Lesson> findByClassGroupIdAndIsDeletedFalseOrderBySortOrder(UUID classGroupId);

    List<Lesson> findByInstitutionIdAndIsDeletedFalse(UUID institutionId);

    @Query("SELECT COUNT(l) FROM Lesson l WHERE l.institutionId IN :institutionIds AND l.isDeleted = false")
    long countByInstitutionIdsAndIsDeletedFalse(@Param("institutionIds") List<UUID> institutionIds);

    @Query("SELECT COUNT(l) FROM Lesson l WHERE l.institutionId IN :institutionIds AND l.isPublished = true AND l.isDeleted = false")
    long countByInstitutionIdsAndPublishedAndIsDeletedFalse(@Param("institutionIds") List<UUID> institutionIds, @Param("published") boolean published);

    @Query("SELECT COUNT(l) FROM Lesson l WHERE l.institutionId IN :institutionIds AND l.termId = :termId AND l.isDeleted = false")
    long countByInstitutionIdsAndTermIdAndIsDeletedFalse(
            @Param("institutionIds") List<UUID> institutionIds,
            @Param("termId") UUID termId);

    @Query("SELECT COUNT(l) FROM Lesson l WHERE l.institutionId IN :institutionIds AND l.termId = :termId AND l.isPublished = true AND l.isDeleted = false")
    long countByInstitutionIdsAndTermIdAndPublishedAndIsDeletedFalse(
            @Param("institutionIds") List<UUID> institutionIds,
            @Param("termId") UUID termId,
            @Param("published") boolean published);

    @Query("SELECT COUNT(l) FROM Lesson l WHERE l.institutionId IN :institutionIds AND l.subjectId = :subjectId AND l.isDeleted = false")
    long countByInstitutionIdsAndSubjectIdAndIsDeletedFalse(
            @Param("institutionIds") List<UUID> institutionIds,
            @Param("subjectId") UUID subjectId);

    @Query("SELECT COUNT(l) FROM Lesson l WHERE l.institutionId IN :institutionIds AND l.subjectId = :subjectId AND l.isPublished = true AND l.isDeleted = false")
    long countByInstitutionIdsAndSubjectIdAndPublishedAndIsDeletedFalse(
            @Param("institutionIds") List<UUID> institutionIds,
            @Param("subjectId") UUID subjectId,
            @Param("published") boolean published);
}