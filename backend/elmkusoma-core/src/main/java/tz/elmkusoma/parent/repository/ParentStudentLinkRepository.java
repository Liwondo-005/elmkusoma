package tz.elmkusoma.parent.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.parent.domain.ParentStudentLink;

import java.util.List;
import java.util.UUID;

@Repository
public interface ParentStudentLinkRepository extends JpaRepository<ParentStudentLink, UUID> {

    @Query("SELECT ps FROM ParentStudentLink ps WHERE ps.parentId = :parentId AND ps.isDeleted = false")
    List<ParentStudentLink> findAllByParentId(@Param("parentId") UUID parentId);

    @Query("SELECT ps FROM ParentStudentLink ps WHERE ps.studentId = :studentId AND ps.isDeleted = false")
    List<ParentStudentLink> findAllByStudentId(@Param("studentId") UUID studentId);

    boolean existsByParentIdAndStudentIdAndIsDeletedFalse(UUID parentId, UUID studentId);
}
