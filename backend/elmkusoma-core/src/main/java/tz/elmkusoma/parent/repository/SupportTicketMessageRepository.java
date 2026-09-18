package tz.elmkusoma.parent.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.parent.domain.SupportTicketMessage;

import java.util.List;
import java.util.UUID;

@Repository
public interface SupportTicketMessageRepository extends JpaRepository<SupportTicketMessage, UUID> {
    List<SupportTicketMessage> findByTicketIdAndIsDeletedFalseOrderByCreatedAtAsc(UUID ticketId);
    long countByTicketIdAndIsDeletedFalse(UUID ticketId);
}
