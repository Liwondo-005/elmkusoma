package tz.elmkusoma.administration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.administration.domain.DashboardSnapshot;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DashboardSnapshotRepository extends JpaRepository<DashboardSnapshot, UUID> {

    Optional<DashboardSnapshot> findByInstitutionIdAndSnapshotTypeAndExpiresAtAfter(
            UUID institutionId, String snapshotType, java.time.LocalDateTime expiresAt);

    Optional<DashboardSnapshot> findFirstByInstitutionIdOrderByGeneratedAtDesc(@Param("institutionId") UUID institutionId);
}
