package tz.elmkusoma.highereducation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tz.elmkusoma.highereducation.domain.WorkshopSession;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface WorkshopSessionRepository extends JpaRepository<WorkshopSession, UUID> {
    List<WorkshopSession> findByStudentIdOrderByScheduledAtDesc(UUID studentId);
    List<WorkshopSession> findByInstitutionIdAndScheduledAtBetween(UUID institutionId, LocalDateTime start, LocalDateTime end);
    List<WorkshopSession> findByInstitutionIdOrderByScheduledAtDesc(UUID institutionId);
}
