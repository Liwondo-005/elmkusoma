package tz.elmkusoma.parent.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.parent.domain.Message;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findBySenderIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID senderId);

    List<Message> findByRecipientIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID recipientId);

    @Query("SELECT m FROM Message m WHERE (m.senderId = :senderId AND m.isDeleted = false) OR (m.recipientId = :recipientId AND m.isDeleted = false) ORDER BY m.createdAt DESC")
    List<Message> findBySenderIdOrRecipientIdAndIsDeletedFalseOrderByCreatedAtDesc(@Param("senderId") UUID senderId, @Param("recipientId") UUID recipientId);

    long countByRecipientIdAndIsDeletedFalseAndIsReadFalse(UUID recipientId);

    long countBySenderIdAndIsDeletedFalse(UUID senderId);
}
