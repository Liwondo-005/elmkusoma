package tz.elmkusoma.parent.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.parent.domain.SupportTicket;

import java.util.List;
import java.util.UUID;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, UUID> {
    List<SupportTicket> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID userId);
    List<SupportTicket> findByUserIdAndStatusAndIsDeletedFalse(UUID userId, String status);
    long countByUserIdAndStatusAndIsDeletedFalse(UUID userId, String status);
    long countByUserIdAndIsDeletedFalse(UUID userId);
    org.springframework.data.domain.Page<SupportTicket> findByIsDeletedFalse(org.springframework.data.domain.Pageable pageable);
    org.springframework.data.domain.Page<SupportTicket> findByStatusAndIsDeletedFalse(String status, org.springframework.data.domain.Pageable pageable);
}
