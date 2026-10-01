package tz.elmkusoma.administration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.administration.domain.ScheduledReport;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ScheduledReportRepository extends JpaRepository<ScheduledReport, UUID> {

    List<ScheduledReport> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID userId);

    @Query("select r from ScheduledReport r where r.isDeleted = false "
            + "and r.status = :status and r.nextRunAt <= :now "
            + "order by r.nextRunAt asc")
    List<ScheduledReport> findDue(@Param("status") String status, @Param("now") LocalDateTime now);
}
