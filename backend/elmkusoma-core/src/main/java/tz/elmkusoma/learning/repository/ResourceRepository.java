package tz.elmkusoma.learning.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.learning.domain.Resource;

import java.util.List;
import java.util.UUID;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, UUID> {

    List<Resource> findBySubjectIdAndIsDeletedFalse(UUID subjectId);

    List<Resource> findByClassGroupIdAndIsDeletedFalse(UUID classGroupId);

    List<Resource> findByInstitutionIdAndIsDeletedFalse(UUID institutionId);

    @Query("SELECT r FROM Resource r WHERE r.isDeleted = false ORDER BY r.createdAt DESC")
    List<Resource> findAllAndIsDeletedFalse();

    @Query("SELECT r FROM Resource r WHERE r.isDeleted = false AND LOWER(r.title) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY r.createdAt DESC")
    List<Resource> searchByTitleAndIsDeletedFalse(@Param("query") String query);

    @Query("SELECT r FROM Resource r WHERE r.isDeleted = false AND r.resourceType = :resourceType ORDER BY r.createdAt DESC")
    List<Resource> findByResourceTypeAndIsDeletedFalse(@Param("resourceType") Resource.ResourceType resourceType);

    @Query("SELECT r FROM Resource r WHERE r.isDeleted = false AND LOWER(r.title) LIKE LOWER(CONCAT('%', :query, '%')) AND r.resourceType = :resourceType ORDER BY r.createdAt DESC")
    List<Resource> searchByTitleAndResourceTypeAndIsDeletedFalse(@Param("query") String query, @Param("resourceType") Resource.ResourceType resourceType);

    @Query("SELECT r FROM Resource r WHERE r.isDeleted = false AND r.id <> :excludeId AND ((r.subjectId IS NOT NULL AND r.subjectId = :subjectId) OR LOWER(r.title) LIKE LOWER(CONCAT('%', :query, '%'))) ORDER BY r.createdAt DESC")
    List<Resource> findRelatedResources(@Param("excludeId") UUID excludeId, @Param("subjectId") UUID subjectId, @Param("query") String query);

    Page<Resource> findByIsDeletedFalse(Pageable pageable);

    Page<Resource> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    @Query("SELECT r FROM Resource r WHERE r.isDeleted = false AND r.institutionId = :institutionId AND (LOWER(r.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(r.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Resource> searchByInstitutionAndQuery(@Param("institutionId") UUID institutionId, @Param("query") String query, Pageable pageable);

    List<Resource> findByInstitutionIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID institutionId);

    Page<Resource> findByInstitutionIdAndIsDeletedFalse(UUID institutionId, Pageable pageable);

    @Query("SELECT r FROM Resource r WHERE r.isDeleted = false AND r.institutionId = :institutionId AND LOWER(r.title) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY r.createdAt DESC")
    List<Resource> searchByInstitutionIdAndIsDeletedFalse(@Param("institutionId") UUID institutionId, @Param("query") String query);
}
