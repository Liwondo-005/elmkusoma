package tz.elmkusoma.course.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.course.domain.Announcement;

import java.util.List;
import java.util.UUID;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, UUID> {

    @Query("SELECT a FROM Announcement a WHERE a.authorId = :authorId AND a.isDeleted = false ORDER BY a.createdAt DESC")
    List<Announcement> findByAuthorIdAndIsDeletedFalse(@Param("authorId") UUID authorId);

    @Query("SELECT a FROM Announcement a WHERE a.institutionId = :institutionId AND a.isDeleted = false ORDER BY a.createdAt DESC")
    List<Announcement> findByInstitutionIdAndIsDeletedFalse(@Param("institutionId") UUID institutionId);

    @Query("SELECT a FROM Announcement a WHERE a.institutionId = :institutionId AND a.classGroupId = :classGroupId AND a.isDeleted = false ORDER BY a.createdAt DESC")
    List<Announcement> findByInstitutionIdAndClassGroupIdAndIsDeletedFalse(
            @Param("institutionId") UUID institutionId, @Param("classGroupId") UUID classGroupId);

    @Query("SELECT a FROM Announcement a WHERE a.institutionId = :institutionId AND a.classGroupId IS NULL AND a.isDeleted = false ORDER BY a.createdAt DESC")
    List<Announcement> findInstitutionWideByInstitutionIdAndIsDeletedFalse(@Param("institutionId") UUID institutionId);

    @Query("SELECT a FROM Announcement a WHERE a.isDeleted = false ORDER BY a.createdAt DESC")
    List<Announcement> findAllAndIsDeletedFalse();
}
