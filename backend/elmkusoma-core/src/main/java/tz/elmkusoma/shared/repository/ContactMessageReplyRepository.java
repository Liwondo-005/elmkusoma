package tz.elmkusoma.shared.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.shared.domain.ContactMessageReply;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContactMessageReplyRepository extends JpaRepository<ContactMessageReply, UUID> {

    List<ContactMessageReply> findByContactMessageIdAndIsDeletedFalseOrderByCreatedAtAsc(UUID contactMessageId);

    /**
     * Replies visible to the enquiry's author: staff replies only.
     *
     * <p>The exclusion happens in the query rather than when mapping the response, so adding a
     * new endpoint cannot leak an internal note by forgetting to filter it in one place.</p>
     */
    @Query("""
            SELECT r FROM ContactMessageReply r
            WHERE r.contactMessageId = :messageId
              AND r.isDeleted = false
              AND r.isInternal = false
            ORDER BY r.createdAt ASC
            """)
    List<ContactMessageReply> findVisibleToRequester(@Param("messageId") UUID messageId);

    Optional<ContactMessageReply> findByIdAndIsDeletedFalse(UUID id);
}