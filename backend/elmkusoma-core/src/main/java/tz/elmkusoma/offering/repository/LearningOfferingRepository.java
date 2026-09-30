package tz.elmkusoma.offering.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.academic.domain.EducationLevel;
import tz.elmkusoma.learning.domain.Resource;
import tz.elmkusoma.offering.domain.LearningOffering;

import java.util.List;
import java.util.UUID;

@Repository
public interface LearningOfferingRepository extends JpaRepository<LearningOffering, UUID> {

    List<LearningOffering> findByOwnerUserIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID ownerUserId);

    @Query("""
            SELECT o FROM LearningOffering o
            WHERE o.isDeleted = false
              AND o.status = 'PUBLISHED'
              AND (o.visibility = :publicVisibility
                   OR (o.visibility = :institutionVisibility
                       AND :institutionId IS NOT NULL
                       AND o.institutionId = :institutionId)
                   OR o.ownerUserId = :userId)
              AND (:q = ''
                   OR LOWER(o.title) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(o.description, '')) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:educationLevel IS NULL OR o.educationLevel = :educationLevel)
              AND (:subjectId IS NULL OR o.subjectId = :subjectId)
              AND (:ownerId IS NULL OR o.ownerUserId = :ownerId)
            ORDER BY o.createdAt DESC
            """)
    Page<LearningOffering> searchDiscoverable(@Param("q") String q,
                                              @Param("educationLevel") EducationLevel educationLevel,
                                              @Param("subjectId") UUID subjectId,
                                              @Param("ownerId") UUID ownerId,
                                              @Param("userId") UUID userId,
                                              @Param("institutionId") UUID institutionId,
                                              @Param("publicVisibility") Resource.ResourceVisibility publicVisibility,
                                              @Param("institutionVisibility") Resource.ResourceVisibility institutionVisibility,
                                              Pageable pageable);
}
