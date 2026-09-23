package tz.elmkusoma.administration.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.administration.domain.ContentReport;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface ContentReportRepository extends JpaRepository<ContentReport, UUID> {
    Page<ContentReport> findByIsDeletedFalse(Pageable pageable);
    Page<ContentReport> findByStatusAndIsDeletedFalse(String status, Pageable pageable);
    long countByStatusAndIsDeletedFalse(String status);
    long countByIsDeletedTrue();

    @Modifying
    @Query("DELETE FROM ContentReport c WHERE c.isDeleted = true AND c.createdAt < :cutoff")
    int purgeSoftDeletedOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
