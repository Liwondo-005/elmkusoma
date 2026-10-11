package tz.elmkusoma.parent.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.parent.domain.SupportTicketMessage;

import java.util.List;
import java.util.UUID;

@Repository
public interface SupportTicketMessageRepository extends JpaRepository<SupportTicketMessage, UUID> {
    List<SupportTicketMessage> findByTicketIdAndIsDeletedFalseOrderByCreatedAtAsc(UUID ticketId);
    long countByTicketIdAndIsDeletedFalse(UUID ticketId);

    /**
     * The conversation as the ticket's own requester may see it.
     *
     * <p>Internal staff notes live in the same table, so the requester-facing read must exclude
     * them in the query rather than filter afterwards. Returning everything here would hand a
     * parent the internal notes staff wrote about them.</p>
     */
    @Query("""
            SELECT m FROM SupportTicketMessage m
            WHERE m.ticketId = :ticketId
              AND m.isDeleted = false
              AND (m.isInternal IS NULL OR m.isInternal = false)
            ORDER BY m.createdAt ASC
            """)
    List<SupportTicketMessage> findVisibleToRequester(
            @org.springframework.data.repository.query.Param("ticketId") UUID ticketId);

    /** Counts only what the requester can see, so a note's existence is not disclosed either. */
    @Query("""
            SELECT COUNT(m) FROM SupportTicketMessage m
            WHERE m.ticketId = :ticketId
              AND m.isDeleted = false
              AND (m.isInternal IS NULL OR m.isInternal = false)
            """)
    long countVisibleToRequester(
            @org.springframework.data.repository.query.Param("ticketId") UUID ticketId);
}
