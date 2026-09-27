package tz.elmkusoma.learning.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.learning.domain.ResourceTagging;

import java.util.List;
import java.util.UUID;

@Repository
public interface ResourceTaggingRepository extends JpaRepository<ResourceTagging, UUID> {

    List<ResourceTagging> findByResourceId(UUID resourceId);

    List<ResourceTagging> findByTagId(UUID tagId);

    @Query("SELECT CASE WHEN COUNT(rt) > 0 THEN true ELSE false END FROM ResourceTagging rt WHERE rt.resourceId = :resourceId AND rt.tagId = :tagId")
    boolean existsByResourceIdAndTagId(@Param("resourceId") UUID resourceId, @Param("tagId") UUID tagId);

    void deleteByResourceId(UUID resourceId);

    void deleteByTagId(UUID tagId);

    void deleteByResourceIdAndTagId(UUID resourceId, UUID tagId);
}