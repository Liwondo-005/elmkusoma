package tz.elmkusoma.learning.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.learning.domain.Resource;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, UUID> {

    List<Resource> findByInstitutionIdAndIsDeletedFalse(UUID institutionId);

    List<Resource> findByLessonIdAndIsDeletedFalse(UUID lessonId);

    List<Resource> findByModuleIdAndIsDeletedFalse(UUID moduleId);

    List<Resource> findByCourseIdAndIsDeletedFalse(UUID courseId);

    List<Resource> findByUploadedByAndIsDeletedFalse(UUID uploadedBy);

    @Query("SELECT r FROM Resource r WHERE r.institutionId = :institutionId AND r.isDeleted = false AND r.visibility IN :visibilities")
    List<Resource> findByInstitutionIdAndVisibilities(@Param("institutionId") UUID institutionId, @Param("visibilities") List<String> visibilities);

    @Query("SELECT r FROM Resource r WHERE r.lessonId = :lessonId AND r.isDeleted = false AND r.visibility IN :visibilities ORDER BY r.sortOrder")
    List<Resource> findVisibleByLessonId(@Param("lessonId") UUID lessonId, @Param("visibilities") List<String> visibilities);

    @Query("SELECT r FROM Resource r WHERE r.moduleId = :moduleId AND r.isDeleted = false AND r.visibility IN :visibilities ORDER BY r.sortOrder")
    List<Resource> findVisibleByModuleId(@Param("moduleId") UUID moduleId, @Param("visibilities") List<String> visibilities);

    @Query("SELECT r FROM Resource r WHERE r.courseId = :courseId AND r.isDeleted = false AND r.visibility IN :visibilities ORDER BY r.sortOrder")
    List<Resource> findVisibleByCourseId(@Param("courseId") UUID courseId, @Param("visibilities") List<String> visibilities);

    Page<Resource> findByInstitutionIdAndIsDeletedFalse(UUID institutionId, org.springframework.data.domain.Pageable pageable);

    long countByInstitutionIdAndIsDeletedFalse(UUID institutionId);

    long countByLessonIdAndIsDeletedFalse(UUID lessonId);

    // Backward compatibility for existing services
    @Query("SELECT r FROM Resource r WHERE r.isDeleted = false")
    Page<Resource> findByIsDeletedFalse(Pageable pageable);

    @Query("SELECT r FROM Resource r WHERE r.isDeleted = false")
    List<Resource> findAllAndIsDeletedFalse();

    // Backward compatibility for existing LearnerController
    @Query("SELECT r FROM Resource r WHERE r.institutionId = :institutionId AND r.isDeleted = false AND (r.title ILIKE %:query% OR r.description ILIKE %:query%)")
    List<Resource> searchByInstitutionIdAndIsDeletedFalse(@Param("institutionId") UUID institutionId, @Param("query") String query);

    // Backward compatibility
    @Query("SELECT r FROM Resource r WHERE r.institutionId = :institutionId AND r.isDeleted = false AND r.visibility = 'PUBLIC' AND (r.title ILIKE %:query% OR r.description ILIKE %:query%)")
    Page<Resource> searchPublishedByInstitutionWithAllFilters(@Param("institutionId") UUID institutionId, @Param("query") String query, @Param("level") String level, @Param("category") String category, @Param("fromDate") java.time.LocalDateTime fromDate, @Param("toDate") java.time.LocalDateTime toDate, Pageable pageable);

    @Query("SELECT r FROM Resource r WHERE r.institutionId = :institutionId AND r.isDeleted = false AND (r.title ILIKE %:query% OR r.description ILIKE %:query%)")
    List<Resource> searchByInstitutionIdAndQuery(@Param("institutionId") UUID institutionId, @Param("query") String query);

    @Query("SELECT r FROM Resource r WHERE r.institutionId = :institutionId AND r.isDeleted = false AND r.id <> :resourceId AND (r.resourceType = :resourceType OR r.lessonId = :lessonId) ORDER BY r.createdAt DESC")
    List<Resource> findRelatedResources(@Param("institutionId") UUID institutionId, @Param("resourceId") UUID resourceId, @Param("lessonId") UUID lessonId, @Param("resourceType") Resource.ResourceType resourceType);

    // Automatic sort order: highest existing order in each resource context
    Optional<Resource> findTopByLessonIdAndIsDeletedFalseOrderBySortOrderDesc(UUID lessonId);

    Optional<Resource> findTopByModuleIdAndIsDeletedFalseOrderBySortOrderDesc(UUID moduleId);

    Optional<Resource> findTopByCourseIdAndIsDeletedFalseOrderBySortOrderDesc(UUID courseId);

    Optional<Resource> findTopByInstitutionIdAndIsDeletedFalseOrderBySortOrderDesc(UUID institutionId);

    @Query("SELECT r FROM Resource r WHERE r.isDeleted = false AND r.institutionId IN :institutionIds ORDER BY r.createdAt DESC")
    Page<Resource> findByInstitutionIdsAndIsDeletedFalse(@Param("institutionIds") java.util.List<UUID> institutionIds, Pageable pageable);

    @Query("SELECT r FROM Resource r WHERE r.isDeleted = false AND r.institutionId IN :institutionIds AND (:query = '' OR LOWER(r.title) LIKE LOWER(CONCAT('%', :query, '%'))) ORDER BY r.createdAt DESC")
    Page<Resource> searchByInstitutionIds(@Param("institutionIds") java.util.List<UUID> institutionIds, @Param("query") String query, Pageable pageable);
}
