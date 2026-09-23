package tz.elmkusoma.administration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.administration.domain.IntegrationStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IntegrationStatusRepository extends JpaRepository<IntegrationStatus, UUID> {
    List<IntegrationStatus> findByIsDeletedFalse();
    Optional<IntegrationStatus> findByIntegrationKeyAndIsDeletedFalse(String integrationKey);
}
