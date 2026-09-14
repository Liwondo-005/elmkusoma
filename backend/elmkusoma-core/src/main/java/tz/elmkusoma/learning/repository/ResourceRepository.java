package tz.elmkusoma.learning.repository;

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
}
