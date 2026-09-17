package tz.elmkusoma.parent.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.parent.domain.Message;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findBySenderIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID senderId);

    List<Message> findByRecipientIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID recipientId);

    List<Message> findBySenderIdOrRecipientIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID senderId, UUID recipientId);

    long countByRecipientIdAndIsDeletedFalseAndIsReadFalse(UUID recipientId);

    long countBySenderIdAndIsDeletedFalse(UUID senderId);
}
