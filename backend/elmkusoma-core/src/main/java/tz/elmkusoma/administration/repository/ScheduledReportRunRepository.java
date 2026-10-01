package tz.elmkusoma.administration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.administration.domain.ScheduledReportRun;

import java.util.List;
import java.util.UUID;

@Repository
public interface ScheduledReportRunRepository extends JpaRepository<ScheduledReportRun, UUID> {

    List<ScheduledReportRun> findByScheduledReportIdOrderByRunAtDesc(UUID scheduledReportId);

    long countByScheduledReportIdAndStatus(UUID scheduledReportId, String status);
}
