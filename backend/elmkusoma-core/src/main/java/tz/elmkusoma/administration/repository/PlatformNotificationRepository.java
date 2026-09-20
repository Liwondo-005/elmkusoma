package tz.elmkusoma.administration.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import tz.elmkusoma.administration.domain.PlatformNotification;

import java.util.UUID;

public interface PlatformNotificationRepository extends JpaRepository<PlatformNotification, UUID> {
    Page<PlatformNotification> findByIsDeletedFalseOrderBySentAtDesc(Pageable pageable);
    long countByIsDeletedFalse();
}
