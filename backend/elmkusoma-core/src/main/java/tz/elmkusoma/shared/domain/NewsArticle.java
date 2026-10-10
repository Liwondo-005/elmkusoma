package tz.elmkusoma.shared.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * A public news article or announcement shown on the landing page.
 *
 * <p>Its own table rather than a row in {@code announcements}: that table is an internal,
 * authenticated, institution-scoped notice feed for learners, addressed by audience region and
 * district. Public marketing content has different visibility rules, a slug-addressed URL and an
 * editorial lifecycle, so the two are kept apart rather than sharing one table and two sets of
 * contradictory predicates.</p>
 *
 * <p>{@code body} holds sanitised rich text. It began life as plain text rendered as React text
 * nodes, which makes stored XSS structurally impossible, but cannot express the bold, links and
 * lists public news needs. {@link tz.elmkusoma.shared.service.NewsContentSanitizer} now cleans
 * the body against a deny-by-default allowlist on write, so this column only ever contains
 * markup that is safe to render as HTML.</p>
 *
 * <p>{@code publishedAt} is written by the server on publish and never read from a request. The
 * NEW indicator is derived from it, so accepting it from a client would let any article claim to
 * be new.</p>
 */
@Entity
@Table(
        name = "news_articles",
        indexes = {
                @Index(name = "idx_news_articles_public", columnList = "status, published_at"),
                @Index(name = "idx_news_articles_status", columnList = "status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class NewsArticle extends tz.elmkusoma.common.BaseEntity {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_PUBLISHED = "PUBLISHED";
    public static final String STATUS_ARCHIVED = "ARCHIVED";

    public static final String PRIORITY_NORMAL = "NORMAL";
    public static final String PRIORITY_IMPORTANT = "IMPORTANT";
    public static final String PRIORITY_URGENT = "URGENT";

    @Column(nullable = false, length = 200)
    private String title;

    /** Short standfirst shown on cards and in listings. Required: a card with no summary reads as broken. */
    @Column(nullable = false, length = 500)
    private String summary;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    /** URL segment for /news/{slug}. Lower-case, hyphen separated, unique among live rows. */
    @Column(nullable = false, length = 160)
    private String slug;

    @Column(length = 40)
    private String category;

    /** Absolute or root-relative image URL. Null is valid: the UI renders a fallback, not a broken image. */
    @Column(name = "cover_image_url", length = 1000)
    private String coverImageUrl;

    /** Publishing organisation, where the article is not attributable to an individual. */
    @Column(name = "author_name", length = 160)
    private String authorName;

    /** DRAFT | PUBLISHED | ARCHIVED. Only PUBLISHED is reachable from a public endpoint. */
    @Column(nullable = false, length = 20)
    @lombok.Builder.Default
    private String status = STATUS_DRAFT;

    @Column(name = "published_at")
    private java.time.LocalDateTime publishedAt;

    /** Publish now, but not before this instant. */
    @Column(name = "scheduled_at")
    private java.time.LocalDateTime scheduledAt;

    /** Once passed the article leaves public listings without anyone unpublishing it. */
    @Column(name = "expires_at")
    private java.time.LocalDateTime expiresAt;

    /**
     * Deliberate editorial choice, never derived from recency. A brand new article is not
     * automatically featured, and an old article can still be featured.
     */
    @Column(name = "is_featured", nullable = false)
    @lombok.Builder.Default
    private Boolean isFeatured = false;

    /** NORMAL | IMPORTANT | URGENT. Kept independent of both recency and featured status. */
    @Column(nullable = false, length = 20)
    @lombok.Builder.Default
    private String priority = PRIORITY_NORMAL;

    /**
     * Optional manual ordering within a featured/priority band. Ties fall through to
     * {@code publishedAt} then {@code id}, so the public listing order is fully deterministic
     * and two articles sharing a position never swap between requests.
     */
    @Column(name = "sort_order", nullable = false)
    @lombok.Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "last_modified_by", length = 255)
    private String lastModifiedBy;

    @Column(name = "published_by", length = 255)
    private String publishedBy;

    public boolean isPublished() {
        return STATUS_PUBLISHED.equals(status);
    }
}