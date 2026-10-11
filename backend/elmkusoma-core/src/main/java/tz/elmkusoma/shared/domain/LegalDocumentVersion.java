package tz.elmkusoma.shared.domain;

import jakarta.persistence.*;
import lombok.experimental.SuperBuilder;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * An immutable snapshot of a legal document at the moment it was published.
 *
 * <p>Rows here are never updated or deleted by the publish path. Publishing inserts the
 * outgoing version before advancing, and a unique index on
 * {@code (legal_document_id, version)} makes a duplicate impossible at the database level. That
 * is what turns "do not silently overwrite historical published content" from a convention into
 * a guarantee: an editor cannot lose a published version even by publishing twice in a row.</p>
 */
@Entity
@Table(name = "legal_document_versions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class LegalDocumentVersion extends tz.elmkusoma.common.BaseEntity {


    @Column(name = "legal_document_id", nullable = false)
    private UUID legalDocumentId;

    @Column(nullable = false)
    private Integer version;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @Builder.Default
    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt = LocalDateTime.now();

    @Column(name = "published_by", length = 255)
    private String publishedBy;
}