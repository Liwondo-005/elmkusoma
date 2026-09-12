package tz.elmkusoma.course.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.course.domain.Course;

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
}
