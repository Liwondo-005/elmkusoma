package tz.elmkusoma.administration.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.administration.domain.WebhookEvent;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, UUID> {

    Page<WebhookEvent> findByIsDeletedFalse(Pageable pageable);

    Page<WebhookEvent> findBySourceAndIsDeletedFalse(String source, Pageable pageable);

    @Query("SELECT w FROM WebhookEvent w WHERE w.isDeleted = false AND (:source IS NULL OR w.source = :source) AND w.processingResult <> 'SUCCESS' ORDER BY w.receivedAt DESC")
    Page<WebhookEvent> findFailed(@Param("source") String source, Pageable pageable);

    long countBySourceAndIsDeletedFalse(String source);

    long countBySourceAndProcessingResultAndIsDeletedFalse(String source, String processingResult);

    WebhookEvent findTopBySourceAndIsDeletedFalseOrderByReceivedAtDesc(String source);

    long countByReceivedAtAfterAndIsDeletedFalse(LocalDateTime after);
}
