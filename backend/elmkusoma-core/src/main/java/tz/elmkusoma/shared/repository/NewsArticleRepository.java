package tz.elmkusoma.shared.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.shared.domain.NewsArticle;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Every public-eligibility rule lives in these queries rather than in controller code.
 *
 * <p>A scheduled article that is not yet due, an expired article, a draft and an archived row are
 * all invisible here regardless of what the caller asks for. Filtering in Java after loading would
 * work, but it would make an unpublished article briefly readable and would put the rule in two
 * places; the spec asks for publication and expiry to be enforced on the server, so the database
 * query is the enforcement point.</p>
 */
@Repository
public interface NewsArticleRepository extends JpaRepository<NewsArticle, java.util.UUID> {

    Optional<NewsArticle> findBySlugAndIsDeletedFalse(String slug);

    boolean existsBySlugAndIsDeletedFalse(String slug);

    /**
     * Public archive listing.
     *
     * <p>Order is deterministic end to end: featured first, then importance, then recency, then
     * the administrator's manual position, then id. Every article has a distinct id, so two rows
     * that agree on featured, priority, publishedAt and sortOrder still come back in the same
     * order on every request - which matters because this feeds a paginated page where a reshuffle
     * would let an item slip between pages.</p>
     *
     * <p>{@code sortOrder} sits below {@code publishedAt} deliberately: it breaks ties within an
     * identical publication date rather than overriding recency.</p>
     */
    @Query("""
            SELECT a FROM NewsArticle a
            WHERE a.isDeleted = false
              AND a.status = 'PUBLISHED'
              AND a.publishedAt IS NOT NULL
              AND a.publishedAt <= :now
              AND (a.scheduledAt IS NULL OR a.scheduledAt <= :now)
              AND (a.expiresAt IS NULL OR a.expiresAt > :now)
            ORDER BY a.isFeatured DESC,
                     CASE a.priority WHEN 'URGENT' THEN 3 WHEN 'IMPORTANT' THEN 2 ELSE 1 END DESC,
                     a.publishedAt DESC,
                     a.sortOrder ASC,
                     a.id ASC
            """)
    Page<NewsArticle> findPublicPage(@Param("now") LocalDateTime now, Pageable pageable);

    /** Landing-page section: the top of the same ordering, uncapped by pagination. */
    @Query("""
            SELECT a FROM NewsArticle a
            WHERE a.isDeleted = false
              AND a.status = 'PUBLISHED'
              AND a.publishedAt IS NOT NULL
              AND a.publishedAt <= :now
              AND (a.scheduledAt IS NULL OR a.scheduledAt <= :now)
              AND (a.expiresAt IS NULL OR a.expiresAt > :now)
            ORDER BY a.isFeatured DESC,
                     CASE a.priority WHEN 'URGENT' THEN 3 WHEN 'IMPORTANT' THEN 2 ELSE 1 END DESC,
                     a.publishedAt DESC,
                     a.sortOrder ASC,
                     a.id ASC
            """)
    List<NewsArticle> findPublicLatest(@Param("now") LocalDateTime now, Pageable pageable);

    /** Single public article by slug. A draft or expired slug returns empty, not a 403. */
    @Query("""
            SELECT a FROM NewsArticle a
            WHERE a.slug = :slug
              AND a.isDeleted = false
              AND a.status = 'PUBLISHED'
              AND a.publishedAt IS NOT NULL
              AND a.publishedAt <= :now
              AND (a.scheduledAt IS NULL OR a.scheduledAt <= :now)
              AND (a.expiresAt IS NULL OR a.expiresAt > :now)
            """)
    Optional<NewsArticle> findPublicBySlug(@Param("slug") String slug, @Param("now") LocalDateTime now);

    /** True when at least one article is publicly visible. Lets the landing section decide hide-vs-empty. */
    @Query("""
            SELECT COUNT(a) > 0 FROM NewsArticle a
            WHERE a.isDeleted = false
              AND a.status = 'PUBLISHED'
              AND a.publishedAt IS NOT NULL
              AND a.publishedAt <= :now
              AND (a.scheduledAt IS NULL OR a.scheduledAt <= :now)
              AND (a.expiresAt IS NULL OR a.expiresAt > :now)
            """)
    boolean hasAnyPublic(@Param("now") LocalDateTime now);

    /**
     * Admin search and filter. Null parameters mean "no filter", so one query covers the
     * unfiltered list, status filter and free-text search without building a specification.
     *
     * <p>{@code CAST(:q AS string)} is not decoration. Inside {@code CONCAT} Postgres has no
     * type context for the parameter, so Hibernate binds a null {@code q} as bytea and the
     * query dies with {@code function lower(bytea) does not exist} - but only when the query is
     * executed before any earlier call has bound {@code q} as a String. That makes it an
     * order-dependent failure: the unfiltered list, which is the first thing the admin screen
     * requests, is exactly the request that breaks on a cold cache.</p>
     */
    @Query("""
            SELECT a FROM NewsArticle a
            WHERE a.isDeleted = false
              AND (:status IS NULL OR a.status = :status)
              AND (:category IS NULL OR a.category = :category)
              AND (:featured IS NULL OR a.isFeatured = :featured)
              AND (:q IS NULL
                   OR LOWER(a.title) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
                   OR LOWER(a.summary) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
                   OR LOWER(a.body) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')))
            ORDER BY a.updatedAt DESC, a.id ASC
            """)
    Page<NewsArticle> searchForAdmin(@Param("status") String status,
                                     @Param("category") String category,
                                     @Param("featured") Boolean featured,
                                     @Param("q") String q,
                                     Pageable pageable);
}