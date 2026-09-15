package tz.elmkusoma.liveclass.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.liveclass.domain.MediaAsset;

import java.util.List;
import java.util.UUID;

@Repository
public interface MediaAssetRepository extends JpaRepository<MediaAsset, UUID> {

    List<MediaAsset> findByInstitutionIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID institutionId);

    List<MediaAsset> findBySourceTypeAndSourceIdAndIsDeletedFalse(String sourceType, UUID sourceId);

    List<MediaAsset> findByTeacherIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID teacherId);

    List<MediaAsset> findByMediaTypeAndIsDeletedFalseOrderByCreatedAtDesc(String mediaType);

    long countByInstitutionIdAndIsDeletedFalse(UUID institutionId);

    @Query("SELECT m FROM MediaAsset m WHERE m.institutionId = :institutionId AND m.isDeleted = false AND (LOWER(m.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(m.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<MediaAsset> searchByTitle(@Param("institutionId") UUID institutionId, @Param("query") String query);
}
