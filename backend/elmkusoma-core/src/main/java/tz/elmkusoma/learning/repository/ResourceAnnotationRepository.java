package tz.elmkusoma.learning.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.learning.domain.ResourceAnnotation;

import java.util.List;
import java.util.UUID;

@Repository
public interface ResourceAnnotationRepository extends JpaRepository<ResourceAnnotation, UUID> {

    Page<ResourceAnnotation> findByResourceIdAndIsDeletedFalse(UUID resourceId, Pageable pageable);

    List<ResourceAnnotation> findByResourceIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID resourceId);

    List<ResourceAnnotation> findByStudentIdAndIsDeletedFalse(UUID studentId);

    @Query("SELECT a FROM ResourceAnnotation a WHERE a.resourceId = :resourceId AND a.isDeleted = false AND a.isPrivate = false ORDER BY a.createdAt DESC")
    List<ResourceAnnotation> findPublicByResourceId(@Param("resourceId") UUID resourceId);

    @Query("SELECT a FROM ResourceAnnotation a WHERE a.parentAnnotationId = :parentId AND a.isDeleted = false ORDER BY a.createdAt")
    List<ResourceAnnotation> findByParentAnnotationId(@Param("parentId") UUID parentId);

    long countByResourceIdAndIsDeletedFalse(UUID resourceId);

    void deleteByResourceId(UUID resourceId);
}