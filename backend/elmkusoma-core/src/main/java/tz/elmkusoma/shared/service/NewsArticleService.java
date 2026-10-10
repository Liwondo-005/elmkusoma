package tz.elmkusoma.shared.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.audit.domain.AuditLog;
import tz.elmkusoma.audit.service.AuditService;
import tz.elmkusoma.common.exception.ResourceNotFoundException;
import tz.elmkusoma.shared.domain.NewsArticle;
import tz.elmkusoma.shared.dto.NewsArticleAdminView;
import tz.elmkusoma.shared.dto.NewsArticleUpsertRequest;
import tz.elmkusoma.shared.dto.NewsArticleView;
import tz.elmkusoma.shared.repository.NewsArticleRepository;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Public news and announcements.
 *
 * <p>Two rules are enforced here rather than left to the client, because both are the kind of thing
 * a client can be trusted to display but not to enforce: an article is publicly visible only while
 * it is PUBLISHED, due and unexpired; and the NEW badge is derived from {@code publishedAt}
 * against a configurable window. A client computing either for itself could mark a two-year-old
 * article as new, or hide an unpublished one.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewsArticleService {

    /**
     * audit_logs.institution_id is NOT NULL, and public news is platform-wide rather than owned by
     * one institution, so platform writes are attributed to the platform institution - the same
     * value CertificateGovernanceService uses for the same reason.
     */
    private static final java.util.UUID PLATFORM_INSTITUTION_ID =
            java.util.UUID.fromString("a0000000-0000-0000-0000-000000000001");

    private static final String NEW_WINDOW_KEY = "news.newWindowDays";
    private static final int DEFAULT_NEW_WINDOW_DAYS = 7;
    private static final int MAX_NEW_WINDOW_DAYS = 365;

    private static final int MAX_BODY_LENGTH = 200_000;
    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_SUMMARY_LENGTH = 500;
    private static final int MAX_SLUG_LENGTH = 160;

    /** Bounded collision retries, mirroring the reference generator in ContactMessageService. */
    private static final int SLUG_ATTEMPTS = 8;

    /**
     * A tag-shaped construct: an opening or closing angle bracket immediately followed by a tag
     * name and eventually a closing bracket.
     *
     * <p>No whitespace is allowed between {@code <} and the tag name, which is what keeps ordinary
     * prose safe - "grades below &lt; 5" and "a &lt; b &gt; c" are not tag-shaped, while
     * {@code <script>}, {@code </p>} and {@code <img src=x>} all are. Requiring the closing
     * bracket also means a stray unmatched {@code <} in prose is left alone.</p>
     *
     * <p>The client already renders bodies as React text nodes, so stored markup could never
     * execute; this is the second of the two barriers, enforced at the boundary rather than
     * assumed downstream.</p>
     */
    private static final Pattern TAG_LIKE = Pattern.compile("</?[a-zA-Z][a-zA-Z0-9]*(\\s[^<>]*)?/?>");

    private final NewsArticleRepository repository;
    private final PublicSiteSettingsService siteSettings;
    private final AuditService auditService;

    // ── public reads ──────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<NewsArticleView> publicPage(int page, int size) {
        int bounded = Math.max(1, Math.min(size, 50));
        Pageable pageable = PageRequest.of(Math.max(0, page), bounded);
        int window = newWindowDays();
        LocalDateTime now = LocalDateTime.now();
        return repository.findPublicPage(now, pageable).map(a -> toView(a, now, window));
    }

    @Transactional(readOnly = true)
    public List<NewsArticleView> publicLatest(int limit) {
        int bounded = Math.max(1, Math.min(limit, 24));
        int window = newWindowDays();
        LocalDateTime now = LocalDateTime.now();
        return repository.findPublicLatest(now, PageRequest.of(0, bounded)).stream()
                .map(a -> toView(a, now, window))
                .toList();
    }

    @Transactional(readOnly = true)
    public NewsArticleView publicBySlug(String slug) {
        LocalDateTime now = LocalDateTime.now();
        NewsArticle article = repository.findPublicBySlug(slug, now)
                // A draft, archived, not-yet-due or expired slug is simply not there. Returning 403
                // would confirm the article exists, which is itself a leak.
                .orElseThrow(() -> new ResourceNotFoundException("NewsArticle", "slug", slug));
        return toView(article, now, newWindowDays());
    }

    // ── admin reads ───────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<NewsArticleAdminView> adminSearch(String status, String category, Boolean featured,
                                                  String q, int page, int size) {
        String normalisedStatus = blankToNull(status);
        String normalisedCategory = blankToNull(category);
        String normalisedQuery = blankToNull(q);
        int bounded = Math.max(1, Math.min(size, 100));
        return repository.searchForAdmin(
                        normalisedStatus, normalisedCategory, featured, normalisedQuery,
                        PageRequest.of(Math.max(0, page), bounded))
                .map(this::toAdminView);
    }

    @Transactional(readOnly = true)
    public NewsArticleAdminView adminGet(java.util.UUID id) {
        return toAdminView(require(id));
    }

    // ── lifecycle ──────────────────────────────────────────────────────────────────────────

    @Transactional
    public NewsArticleAdminView create(NewsArticleUpsertRequest request, String actor) {
        validate(request);

        NewsArticle article = NewsArticle.builder()
                .title(request.getTitle().trim())
                .summary(request.getSummary().trim())
                .body(request.getBody())
                .slug(resolveSlug(request.getSlug(), request.getTitle()))
                .category(trimToNull(request.getCategory()))
                .coverImageUrl(trimToNull(request.getCoverImageUrl()))
                .authorName(trimToNull(request.getAuthorName()))
                .status(NewsArticle.STATUS_DRAFT)
                .priority(request.getPriority() == null ? NewsArticle.PRIORITY_NORMAL : request.getPriority())
                .isFeatured(Boolean.TRUE.equals(request.getFeatured()))
                .sortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder())
                .scheduledAt(request.getScheduledAt())
                .expiresAt(request.getExpiresAt())
                .lastModifiedBy(actor)
                .build();

        NewsArticle saved = repository.save(article);
        audit(saved, actor, null, AuditLog.AuditAction.CREATE);
        log.info("News article created: slug={}, actor={}", saved.getSlug(), actor);
        return toAdminView(saved);
    }

    @Transactional
    public NewsArticleAdminView update(java.util.UUID id, NewsArticleUpsertRequest request, String actor) {
        validate(request);
        NewsArticle article = require(id);

        Map<String, Object> before = snapshot(article);

        article.setTitle(request.getTitle().trim());
        article.setSummary(request.getSummary().trim());
        article.setBody(request.getBody());
        // An article that already has a URL keeps it. Re-deriving the slug on every edit would
        // silently break any link already shared for the article.
        if (blankToNull(request.getSlug()) != null) {
            String requested = request.getSlug().trim();
            if (!requested.equals(article.getSlug())) {
                ensureSlugAvailable(requested, article.getId());
                article.setSlug(requested);
            }
        }
        article.setCategory(trimToNull(request.getCategory()));
        article.setCoverImageUrl(trimToNull(request.getCoverImageUrl()));
        article.setAuthorName(trimToNull(request.getAuthorName()));
        article.setPriority(request.getPriority() == null ? article.getPriority() : request.getPriority());
        if (request.getFeatured() != null) {
            article.setIsFeatured(request.getFeatured());
        }
        if (request.getSortOrder() != null) {
            article.setSortOrder(request.getSortOrder());
        }
        article.setScheduledAt(request.getScheduledAt());
        article.setExpiresAt(request.getExpiresAt());
        article.setLastModifiedBy(actor);

        NewsArticle saved = repository.save(article);
        audit(saved, actor, before, AuditLog.AuditAction.UPDATE);
        return toAdminView(saved);
    }

    /**
     * Publishing stamps {@code publishedAt} on the server. A caller cannot supply it, so NEW can
     * only ever be earned by actually publishing.
     */
    @Transactional
    public NewsArticleAdminView publish(java.util.UUID id, String actor) {
        NewsArticle article = require(id);
        Map<String, Object> before = snapshot(article);

        if (NewsArticle.STATUS_ARCHIVED.equals(article.getStatus())) {
            throw new IllegalStateException(
                    "An archived article cannot be published. Restore it to draft first.");
        }

        article.setStatus(NewsArticle.STATUS_PUBLISHED);
        article.setPublishedAt(LocalDateTime.now());
        article.setPublishedBy(actor);
        article.setLastModifiedBy(actor);

        NewsArticle saved = repository.save(article);
        audit(saved, actor, before, AuditLog.AuditAction.PUBLISH);
        log.info("News article published: slug={}, actor={}", saved.getSlug(), actor);
        return toAdminView(saved);
    }

    /**
     * Withdraws an article from the public site without discarding it. {@code publishedAt} is
     * left in place so the audit trail keeps the date it was live; re-publishing stamps a fresh
     * one rather than resurrecting the old date.
     */
    @Transactional
    public NewsArticleAdminView unpublish(java.util.UUID id, String actor) {
        NewsArticle article = require(id);
        if (!NewsArticle.STATUS_PUBLISHED.equals(article.getStatus())) {
            throw new IllegalStateException("Only a published article can be unpublished");
        }
        Map<String, Object> before = snapshot(article);

        article.setStatus(NewsArticle.STATUS_DRAFT);
        article.setLastModifiedBy(actor);

        NewsArticle saved = repository.save(article);
        audit(saved, actor, before, AuditLog.AuditAction.UNPUBLISH);
        log.info("News article unpublished: slug={}, actor={}", saved.getSlug(), actor);
        return toAdminView(saved);
    }

    @Transactional
    public NewsArticleAdminView archive(java.util.UUID id, String actor) {
        NewsArticle article = require(id);
        Map<String, Object> before = snapshot(article);

        article.setStatus(NewsArticle.STATUS_ARCHIVED);
        article.setLastModifiedBy(actor);

        NewsArticle saved = repository.save(article);
        audit(saved, actor, before, AuditLog.AuditAction.ARCHIVE);
        log.info("News article archived: slug={}, actor={}", saved.getSlug(), actor);
        return toAdminView(saved);
    }

    @Transactional
    public void delete(java.util.UUID id, String actor) {
        NewsArticle article = require(id);
        Map<String, Object> before = snapshot(article);

        // Soft delete, matching every other content entity here. The row survives for audit.
        article.setIsDeleted(true);
        article.setLastModifiedBy(actor);
        NewsArticle saved = repository.save(article);
        audit(saved, actor, before, AuditLog.AuditAction.DELETE);
        log.info("News article deleted: slug={}, actor={}", saved.getSlug(), actor);
    }

    /** Toggles the featured star. Never derived from recency - this is an explicit editorial act. */
    @Transactional
    public NewsArticleAdminView setFeatured(java.util.UUID id, boolean featured, String actor) {
        NewsArticle article = require(id);
        Map<String, Object> before = snapshot(article);

        article.setIsFeatured(featured);
        article.setLastModifiedBy(actor);

        NewsArticle saved = repository.save(article);
        audit(saved, actor, before, AuditLog.AuditAction.FEATURE);
        return toAdminView(saved);
    }

    // ── helpers ────────────────────────────────────────────────────────────────────────────

    private NewsArticle require(java.util.UUID id) {
        return repository.findById(id)
                .filter(a -> !Boolean.TRUE.equals(a.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("NewsArticle", "id", id));
    }

    /**
     * Resolves the URL segment.
     *
     * <p>An explicitly supplied slug that is already taken is a conflict and is reported, because
     * quietly changing an administrator's chosen URL is worse than refusing it. A slug derived
     * from the title collides freely, so that case gets a numbered suffix instead.</p>
     */
    private String resolveSlug(String requested, String title) {
        if (blankToNull(requested) != null) {
            String slug = requested.trim();
            ensureSlugAvailable(slug, null);
            return slug;
        }

        String base = slugify(title);
        if (base.isEmpty()) {
            base = "article";
        }
        if (!repository.existsBySlugAndIsDeletedFalse(base)) {
            return base;
        }
        for (int suffix = 2; suffix <= SLUG_ATTEMPTS; suffix++) {
            String candidate = base + "-" + suffix;
            if (candidate.length() <= MAX_SLUG_LENGTH && !repository.existsBySlugAndIsDeletedFalse(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException(
                "Could not derive a unique slug from this title. Please supply a slug.");
    }

    private void ensureSlugAvailable(String slug, java.util.UUID selfId) {
        repository.findBySlugAndIsDeletedFalse(slug).ifPresent(existing -> {
            if (selfId == null || !existing.getId().equals(selfId)) {
                throw new IllegalStateException("The slug '" + slug + "' is already used by another article");
            }
        });
    }

    /**
     * Lower-cases, strips accents and collapses everything that is not alphanumeric into single
     * hyphens. Accent stripping matters because a Kiswahili or accented title would otherwise
     * produce a URL the browser and the server disagree about.
     */
    static String slugify(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String normalised = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        String slug = normalised.toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (slug.length() > MAX_SLUG_LENGTH) {
            slug = slug.substring(0, MAX_SLUG_LENGTH).replaceAll("-+$", "");
        }
        return slug;
    }

    /**
     * Service-layer validation, duplicated from the DTO annotations on purpose: a value can also
     * arrive by direct database edit or migration, and the same checks then still apply.
     */
    private void validate(NewsArticleUpsertRequest request) {
        requireLength("Title", request.getTitle(), MAX_TITLE_LENGTH);
        requireLength("Summary", request.getSummary(), MAX_SUMMARY_LENGTH);
        requireLength("Body", request.getBody(), MAX_BODY_LENGTH);
        requirePlainText("Title", request.getTitle());
        requirePlainText("Summary", request.getSummary());
        requirePlainText("Body", request.getBody());

        if (request.getExpiresAt() != null && request.getScheduledAt() != null
                && !request.getExpiresAt().isAfter(request.getScheduledAt())) {
            throw new IllegalArgumentException("Expiry must be after the scheduled publication time");
        }
        if (request.getExpiresAt() != null && !request.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "Expiry must be in the future; an article that has already expired would never be public");
        }
        if (request.getSortOrder() != null && request.getSortOrder() < 0) {
            throw new IllegalArgumentException("Sort order must not be negative");
        }
    }

    private static void requireLength(String field, String value, int max) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        if (value.length() > max) {
            throw new IllegalArgumentException(field + " exceeds " + max + " characters");
        }
    }

    /**
     * The NEW window, in days.
     *
     * <p>Read fresh on each call rather than cached at startup so an administrator can change it
     * without a redeploy. A missing or unparseable value falls back to the default instead of
     * failing the listing - a bad config value must not take the news section down.</p>
     */
    private int newWindowDays() {
        String raw = siteSettings.rawValue(NEW_WINDOW_KEY);
        if (raw == null || raw.isBlank()) {
            return DEFAULT_NEW_WINDOW_DAYS;
        }
        try {
            return Math.max(0, Math.min(Integer.parseInt(raw.trim()), MAX_NEW_WINDOW_DAYS));
        } catch (NumberFormatException ex) {
            log.warn("{} is not a number ('{}'); using {} days", NEW_WINDOW_KEY, raw, DEFAULT_NEW_WINDOW_DAYS);
            return DEFAULT_NEW_WINDOW_DAYS;
        }
    }

    private NewsArticleView toView(NewsArticle a, LocalDateTime now, int windowDays) {
        boolean isNew = windowDays > 0
                && a.getPublishedAt() != null
                && a.getPublishedAt().isAfter(now.minusDays(windowDays));

        // updatedAt is stamped by the audit listener on every save, so it matches publishedAt
        // immediately after publishing. Reporting that as "last updated" would show visitors two
        // identical dates for every article that has never been edited.
        LocalDateTime updated = a.getUpdatedAt();
        if (updated != null && updated.equals(a.getPublishedAt())) {
            updated = null;
        }

        return NewsArticleView.builder()
                .id(a.getId().toString())
                .slug(a.getSlug())
                .title(a.getTitle())
                .summary(a.getSummary())
                .body(a.getBody())
                .category(a.getCategory())
                .coverImageUrl(a.getCoverImageUrl())
                .authorName(a.getAuthorName())
                .publishedAt(a.getPublishedAt())
                .updatedAt(updated)
                .featured(Boolean.TRUE.equals(a.getIsFeatured()))
                .priority(a.getPriority())
                .isNew(isNew)
                .build();
    }

    private NewsArticleAdminView toAdminView(NewsArticle a) {
        return NewsArticleAdminView.builder()
                .id(a.getId().toString())
                .slug(a.getSlug())
                .title(a.getTitle())
                .summary(a.getSummary())
                .body(a.getBody())
                .category(a.getCategory())
                .coverImageUrl(a.getCoverImageUrl())
                .authorName(a.getAuthorName())
                .status(a.getStatus())
                .publishedAt(a.getPublishedAt())
                .scheduledAt(a.getScheduledAt())
                .expiresAt(a.getExpiresAt())
                .featured(Boolean.TRUE.equals(a.getIsFeatured()))
                .priority(a.getPriority())
                .sortOrder(a.getSortOrder())
                .lastModifiedBy(a.getLastModifiedBy())
                .publishedBy(a.getPublishedBy())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }

    private Map<String, Object> snapshot(NewsArticle a) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("slug", a.getSlug());
        values.put("title", a.getTitle());
        values.put("status", a.getStatus());
        values.put("featured", Boolean.TRUE.equals(a.getIsFeatured()));
        values.put("priority", a.getPriority());
        return values;
    }

    /**
     * Audit write that can never fail the business transaction. A logging problem must not be
     * able to lose a saved article.
     */
    private void audit(NewsArticle article, String actor, Map<String, Object> before, AuditLog.AuditAction action) {
        try {
            auditService.recordAuditLog(
                    PLATFORM_INSTITUTION_ID, null, actor, "ADMIN",
                    "NEWS_ARTICLE", article.getId(), article.getTitle(),
                    action, before, snapshot(article));
        } catch (Exception ex) {
            log.warn("Audit write failed for NEWS_ARTICLE {} {}: {}", action, article.getId(), ex.getMessage());
        }
    }

    private static void requirePlainText(String field, String value) {
        if (TAG_LIKE.matcher(value).find()) {
            throw new IllegalArgumentException(
                    field + " must be plain text. HTML markup is not accepted in news content.");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String trimToNull(String value) {
        return blankToNull(value);
    }
}