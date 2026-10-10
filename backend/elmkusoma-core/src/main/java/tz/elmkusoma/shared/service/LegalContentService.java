package tz.elmkusoma.shared.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.common.exception.ResourceNotFoundException;
import tz.elmkusoma.shared.domain.LegalDocument;
import tz.elmkusoma.shared.domain.LegalDocumentVersion;
import tz.elmkusoma.shared.repository.LegalDocumentRepository;
import tz.elmkusoma.shared.repository.LegalDocumentVersionRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Legal documents: a draft working copy plus an append-only publication history.
 *
 * <p>Publishing archives whatever is currently live <em>before</em> advancing, so a published
 * version is never mutated. Reverting does not roll history back either: it copies an old
 * version's text into the working copy, and the next publish records it as a new version. That
 * way "what was published, and when" stays answerable forever, which is the point of keeping
 * legal text under change control at all.</p>
 *
 * <p>The public read path returns the archived version only. The working copy is never served
 * anonymously, so an in-progress edit cannot leak.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LegalContentService {

    private static final Set<String> VALID_TYPES = Set.of(
            LegalDocument.TYPE_TERMS,
            LegalDocument.TYPE_PRIVACY,
            LegalDocument.TYPE_COOKIE,
            LegalDocument.TYPE_SUPPORT_POLICY);

    private static final int MAX_CONTENT_LENGTH = 200_000;

    private final LegalDocumentRepository documentRepository;
    private final LegalDocumentVersionRepository versionRepository;

    // ── working copy ──────────────────────────────────────────────────────────────────────

    @Transactional
    public LegalDocument createDraft(String docType, String title, String content,
                                     java.time.LocalDate effectiveDate, String actor) {
        String type = normaliseType(docType);
        documentRepository.findByDocTypeAndIsDeletedFalse(type).ifPresent(existing -> {
            throw new IllegalStateException("A " + type + " document already exists; edit it instead");
        });
        validateText(title, content);
        LegalDocument saved = documentRepository.save(LegalDocument.builder()
                .docType(type)
                .title(title.trim())
                .content(content.trim())
                .effectiveDate(effectiveDate)
                .version(1)
                .lastModifiedBy(actor)
                .build());
        log.info("Legal draft created: type={}, by={}", type, actor);
        return saved;
    }

    /**
     * Edits the working copy. This never changes what the public sees - publication is a
     * separate, explicit step.
     */
    @Transactional
    public LegalDocument updateDraft(UUID id, String title, String content,
                                     java.time.LocalDate effectiveDate, String actor) {
        LegalDocument document = require(id);
        validateText(title, content);
        document.setTitle(title.trim());
        document.setContent(content.trim());
        document.setEffectiveDate(effectiveDate);
        document.setVersion((document.getVersion() == null ? 1 : document.getVersion()) + 1);
        document.setLastModifiedBy(actor);
        LegalDocument saved = documentRepository.save(document);
        log.info("Legal draft updated: type={}, v{}, by={}", saved.getDocType(), saved.getVersion(), actor);
        return saved;
    }

    // ── publishing ────────────────────────────────────────────────────────────────────────

    /**
     * Publishes the working copy as the next version.
     *
     * <p>Idempotent by content: publishing twice without an intervening edit is refused rather
     * than burning version numbers, so a double-click cannot produce two identical live
     * versions.</p>
     */
    @Transactional
    public LegalDocument publish(UUID id, String actor) {
        LegalDocument document = require(id);
        int nextVersion = (document.getPublishedVersion() == null ? 0 : document.getPublishedVersion()) + 1;

        if (document.getPublishedVersion() != null) {
            LegalDocumentVersion current = versionRepository
                    .findByLegalDocumentIdAndVersionAndIsDeletedFalse(document.getId(), document.getPublishedVersion())
                    .orElse(null);
            if (current != null && current.getContent().equals(document.getContent())
                    && current.getTitle().equals(document.getTitle())) {
                throw new IllegalStateException(
                        "Nothing has changed since v" + document.getPublishedVersion()
                                + " was published; edit the draft before publishing again");
            }
        }

        // Archive first. If this insert fails the whole transaction unwinds and the previously
        // published version stays live, so a visitor never sees a document with no archive.
        versionRepository.save(LegalDocumentVersion.builder()
                .legalDocumentId(document.getId())
                .version(nextVersion)
                .title(document.getTitle())
                .content(document.getContent())
                .effectiveDate(document.getEffectiveDate())
                .publishedAt(LocalDateTime.now())
                .publishedBy(actor)
                .build());

        document.setPublishedVersion(nextVersion);
        document.setLastModifiedBy(actor);
        LegalDocument saved = documentRepository.save(document);
        log.info("Legal document published: type={}, v{}", saved.getDocType(), nextVersion);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<LegalDocumentVersion> history(UUID id) {
        require(id);
        return versionRepository.findByLegalDocumentIdAndIsDeletedFalseOrderByVersionDesc(id);
    }

    /**
     * Restores an earlier version into the working copy as a new draft.
     *
     * <p>Deliberately not a rollback: the published history stays intact and the restored text
     * becomes the next version when published, so the record shows that v1 was republished as
     * v4 rather than pretending v1 is live again.</p>
     */
    @Transactional
    public LegalDocument revert(UUID id, int versionNumber, String actor) {
        LegalDocument document = require(id);
        LegalDocumentVersion target = versionRepository
                .findByLegalDocumentIdAndVersionAndIsDeletedFalse(id, versionNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "LegalDocumentVersion", "version", versionNumber));
        document.setTitle(target.getTitle());
        document.setContent(target.getContent());
        document.setEffectiveDate(target.getEffectiveDate());
        document.setVersion((document.getVersion() == null ? 1 : document.getVersion()) + 1);
        document.setLastModifiedBy(actor);
        LegalDocument saved = documentRepository.save(document);
        log.info("Legal document reverted to v{} content (type={}); draft is now v{}, publish to apply",
                versionNumber, saved.getDocType(), saved.getVersion());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<LegalDocument> listAll() {
        return documentRepository.findAllByIsDeletedFalseOrderByDocTypeAsc();
    }

    @Transactional(readOnly = true)
    public LegalDocument get(UUID id) {
        return require(id);
    }

    // ── public read path ──────────────────────────────────────────────────────────────────

    /**
     * The published version of a document type, or null when nothing has been published.
     *
     * <p>Reads the archive, never the working copy, so an unpublished edit is unreachable from
     * the public endpoint.</p>
     */
    @Transactional(readOnly = true)
    public LegalDocumentVersion publishedVersion(String docType) {
        String type = normaliseType(docType);
        return documentRepository.findByDocTypeAndIsDeletedFalse(type)
                .filter(doc -> doc.getPublishedVersion() != null)
                .flatMap(doc -> versionRepository.findByLegalDocumentIdAndVersionAndIsDeletedFalse(
                        doc.getId(), doc.getPublishedVersion()))
                .orElse(null);
    }

    /** Valid document types, for admin UI dropdowns and error messages. */
    public static Set<String> validTypes() {
        return VALID_TYPES;
    }

    // ── helpers ───────────────────────────────────────────────────────────────────────────

    private LegalDocument require(UUID id) {
        return documentRepository.findById(id)
                .filter(d -> !Boolean.TRUE.equals(d.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("LegalDocument", "id", id));
    }

    private static String normaliseType(String docType) {
        String type = docType == null ? "" : docType.trim().toUpperCase(Locale.ROOT);
        if (!VALID_TYPES.contains(type)) {
            throw new IllegalArgumentException(
                    "Unknown legal document type. Expected one of: " + String.join(", ", VALID_TYPES));
        }
        return type;
    }

    /**
     * Length and content-shape checks.
     *
     * <p>Legal content is plain text by contract: the API never accepts HTML and the client
     * renders it as text nodes, so there is no markup to sanitise. The control characters
     * check keeps a pasted binary blob out of a field a human has to read.</p>
     */
    private static void validateText(String title, String content) {
        if (title == null || title.trim().isEmpty() || title.trim().length() > 200) {
            throw new IllegalArgumentException("Title must be between 1 and 200 characters");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Content is required");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("Content exceeds " + MAX_CONTENT_LENGTH + " characters");
        }
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            // Tab, newline and carriage return are legitimate; anything else in C0 is not.
            if (c < 0x20 && c != '\n' && c != '\r' && c != '\t') {
                throw new IllegalArgumentException("Content contains unsupported control characters");
            }
        }
    }
}