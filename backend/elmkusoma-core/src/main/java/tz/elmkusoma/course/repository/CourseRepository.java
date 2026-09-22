package tz.elmkusoma.course.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.course.domain.Course;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface CourseRepository extends JpaRepository<Course, UUID> {

    List<Course> findByInstitutionIdAndIsDeletedFalse(UUID institutionId);

    @Query("SELECT c FROM Course c WHERE c.institutionId = :institutionId AND c.isDeleted = false ORDER BY c.createdAt DESC")
    List<Course> findAllByInstitutionIdOrderByCreatedAtDesc(@Param("institutionId") UUID institutionId);

    List<Course> findByInstitutionIdAndLevelAndIsDeletedFalse(UUID institutionId, String level);

    List<Course> findByInstitutionIdAndIsPublishedAndIsDeletedFalse(UUID institutionId, Boolean isPublished);

    List<Course> findByInstitutionIdAndIsFeaturedAndIsDeletedFalse(UUID institutionId, Boolean isFeatured);

    long countByInstitutionIdAndIsDeletedFalse(UUID institutionId);

    long countByInstitutionIdAndIsPublishedAndIsDeletedFalse(UUID institutionId, Boolean isPublished);

    long countByInstitutionIdAndLevelAndIsDeletedFalse(UUID institutionId, String level);

    long countByInstitutionIdAndIsFeaturedAndIsDeletedFalse(UUID institutionId, Boolean isFeatured);

    boolean existsByInstitutionIdAndTitleAndIsDeletedFalse(UUID institutionId, String title);

    @Query("SELECT c FROM Course c WHERE c.isDeleted = false AND c.isPublished = true ORDER BY c.createdAt DESC")
    List<Course> findAllPublishedAndIsDeletedFalse();

    @Query("SELECT c FROM Course c WHERE c.isDeleted = false AND c.isPublished = true AND LOWER(c.title) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY c.createdAt DESC")
    List<Course> searchByTitleAndIsDeletedFalse(@Param("query") String query);

    @Query("SELECT c FROM Course c WHERE c.isDeleted = false ORDER BY c.createdAt DESC")
    List<Course> findAllAndIsDeletedFalse();

    @Query("SELECT c FROM Course c WHERE c.isDeleted = false AND c.isPublished = true AND LOWER(c.title) LIKE LOWER(CONCAT('%', :query, '%')) AND (:level IS NULL OR c.level = :level) AND (:category IS NULL OR LOWER(c.category) = LOWER(:category)) ORDER BY c.createdAt DESC")
    List<Course> searchPublishedWithFilters(@Param("query") String query, @Param("level") String level, @Param("category") String category);

    @Query("SELECT c FROM Course c WHERE c.isDeleted = false AND c.isPublished = true AND LOWER(c.title) LIKE LOWER(CONCAT('%', :query, '%')) AND (:level IS NULL OR c.level = :level) AND (:category IS NULL OR LOWER(c.category) = LOWER(:category)) AND (:providerId IS NULL OR c.institutionId = :providerId) AND (:dateFrom IS NULL OR c.createdAt >= :dateFrom) AND (:dateTo IS NULL OR c.createdAt <= :dateTo) ORDER BY c.createdAt DESC")
    List<Course> searchPublishedWithAllFilters(@Param("query") String query, @Param("level") String level, @Param("category") String category, @Param("providerId") UUID providerId, @Param("dateFrom") LocalDateTime dateFrom, @Param("dateTo") LocalDateTime dateTo);

    @Query("SELECT c FROM Course c WHERE c.isDeleted = false AND c.isPublished = true AND c.institutionId = :institutionId AND LOWER(c.title) LIKE LOWER(CONCAT('%', :query, '%')) AND (:level IS NULL OR c.level = :level) AND (:category IS NULL OR LOWER(c.category) = LOWER(:category)) AND (:dateFrom IS NULL OR c.createdAt >= :dateFrom) AND (:dateTo IS NULL OR c.createdAt <= :dateTo) ORDER BY c.createdAt DESC")
    List<Course> searchPublishedByInstitutionWithAllFilters(@Param("institutionId") UUID institutionId, @Param("query") String query, @Param("level") String level, @Param("category") String category, @Param("dateFrom") LocalDateTime dateFrom, @Param("dateTo") LocalDateTime dateTo);

    @Query("SELECT c FROM Course c WHERE c.isDeleted = false AND c.isPublished = true AND c.id <> :excludeId AND (LOWER(c.title) LIKE LOWER(CONCAT('%', :query, '%')) OR (c.level = :level) OR (LOWER(c.category) = LOWER(:category))) ORDER BY c.createdAt DESC")
    List<Course> findRelatedPublishedCourses(@Param("excludeId") UUID excludeId, @Param("query") String query, @Param("level") String level, @Param("category") String category);

    @Query("SELECT c FROM Course c WHERE c.isDeleted = false AND c.isPublished = true AND c.level = :level ORDER BY c.createdAt DESC")
    List<Course> findAllPublishedByLevelAndIsDeletedFalse(@Param("level") String level);

    @Query("SELECT c FROM Course c WHERE c.isDeleted = false AND c.isPublished = true AND LOWER(c.category) = LOWER(:category) ORDER BY c.createdAt DESC")
    List<Course> findAllPublishedByCategoryAndIsDeletedFalse(@Param("category") String category);

    Page<Course> findByIsPublishedAndIsDeletedFalse(Boolean isPublished, Pageable pageable);

    @Query("SELECT c FROM Course c WHERE c.isDeleted = false AND c.isPublished = true AND c.institutionId = :institutionId ORDER BY c.createdAt DESC")
    Page<Course> findByInstitutionIdAndStatusAndIsDeletedFalse(@Param("institutionId") UUID institutionId, @Param("status") String status, Pageable pageable);
}
