package tz.elmkusoma.shared.domain;

import jakarta.persistence.*;
import lombok.experimental.SuperBuilder;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * The working copy of a legal document: Terms &amp; Conditions, Privacy Policy, Cookie policy,
 * Support policy.
 *
 * <p>The split follows the pattern already used for certificate templates: this row holds the
 * draft that an editor works on, and every publication is copied into
 * {@link LegalDocumentVersion} before the working copy is allowed to move on. Visitors only ever
 * read the published archive, never this table.</p>
 *
 * <p>{@code content} is plain text on purpose. The build has no HTML sanitiser available, so
 * rather than accept markup and hope a filter catches it, legal content is refused as HTML and
 * rendered as text by the client. Stored XSS has no surface to attach to.</p>
 */
@Entity
@Table(name = "legal_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class LegalDocument extends tz.elmkusoma.common.BaseEntity {

    /** TERMS | PRIVACY | COOKIE | SUPPORT_POLICY */
    public static final String TYPE_TERMS = "TERMS";
    public static final String TYPE_PRIVACY = "PRIVACY";
    public static final String TYPE_COOKIE = "COOKIE";
    public static final String TYPE_SUPPORT_POLICY = "SUPPORT_POLICY";


    @Column(name = "doc_type", nullable = false, length = 32)
    private String docType;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    /** Working-copy revision counter, incremented on every save. */
    @Builder.Default
    @Column(nullable = false)
    private Integer version = 1;

    /** Highest published version number. Null until the document has been published once. */
    @Column(name = "published_version")
    private Integer publishedVersion;

    @Column(name = "last_modified_by", length = 255)
    private String lastModifiedBy;
}