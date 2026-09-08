package tz.elmkusoma.audit.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.audit.domain.ActivityFeed;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ActivityFeedRepository extends JpaRepository<ActivityFeed, UUID> {

    @Query("SELECT a FROM ActivityFeed a WHERE a.institutionId = :institutionId ORDER BY a.createdAt DESC")
    Page<ActivityFeed> findByInstitutionId(@Param("institutionId") UUID institutionId, Pageable pageable);

    @Query("SELECT a FROM ActivityFeed a WHERE a.userId = :userId ORDER BY a.createdAt DESC")
    Page<ActivityFeed> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT a FROM ActivityFeed a WHERE a.institutionId = :institutionId AND a.createdAt BETWEEN :from AND :to ORDER BY a.createdAt DESC")
    List<ActivityFeed> findByInstitutionIdAndDateRange(@Param("institutionId") UUID institutionId,
                                                        @Param("from") LocalDateTime from,
                                                        @Param("to") LocalDateTime to);
}
