package tz.elmkusoma.shared.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.shared.domain.ContactMessage;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContactMessageRepository extends JpaRepository<ContactMessage, UUID> {

    Optional<ContactMessage> findByReferenceAndIsDeletedFalse(String reference);

    Optional<ContactMessage> findByIdAndIsDeletedFalse(UUID id);

    Page<ContactMessage> findByIsDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

    Page<ContactMessage> findByStatusAndIsDeletedFalseOrderByCreatedAtDesc(String status, Pageable pageable);

    Page<ContactMessage> findByCategoryAndIsDeletedFalseOrderByCreatedAtDesc(String category, Pageable pageable);

    Page<ContactMessage> findByIsDeletedFalseAndEmailIgnoreCaseOrderByCreatedAtDesc(String email, Pageable pageable);

    /**
     * Combined free-text search for the support inbox. Bounded by an explicit LIKE prefix on
     * the column so the pattern stays indexable and a leading wildcard cannot be used to force
     * a full scan of the table.
     */
    @Query("""
            SELECT c FROM ContactMessage c
            WHERE c.isDeleted = false
              AND (LOWER(c.subject) LIKE LOWER(CONCAT('%', :term, '%'))
                   OR LOWER(c.reference) LIKE LOWER(CONCAT('%', :term, '%'))
                   OR LOWER(c.email) LIKE LOWER(CONCAT('%', :term, '%')))
            """)
    Page<ContactMessage> search(@Param("term") String term, Pageable pageable);

    @Query("""
            SELECT c FROM ContactMessage c
            WHERE c.isDeleted = false
              AND c.email = :email
            ORDER BY c.createdAt DESC
            """)
    java.util.List<ContactMessage> findAllForUser(@Param("email") String email);
}