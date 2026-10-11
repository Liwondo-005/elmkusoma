package tz.elmkusoma.shared.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.audit.domain.AuditLog;
import tz.elmkusoma.audit.service.AuditService;
import tz.elmkusoma.common.exception.ResourceNotFoundException;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.course.domain.LiveClass;
import tz.elmkusoma.course.repository.LiveClassRepository;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Editorial publication of live classes.
 *
 * <p>The audit found no approval workflow at all: {@code createLiveClass} set SCHEDULED
 * unconditionally, so a class became schedulable with no editorial gate at all. This adds the
 * minimal workflow the brief calls for, consistent with Platform Admin authorization and the
 * existing audit mechanism.</p>
 *
 * <p>Two independent decisions, deliberately:</p>
 * <ol>
 *   <li><b>Editorial</b>: DRAFT → PENDING_APPROVAL → APPROVED → PUBLISHED (or REJECTED). Owns
 *       "should this be announced".</li>
 *   <li><b>Audience</b>: accessLevel. Owns "who may discover it".</li>
 * </ol>
 *
 * <p>Publishing does not set accessLevel to PUBLIC, and setting accessLevel to PUBLIC does not
 * publish. A class is only publicly discoverable when it is both PUBLISHED and PUBLIC, which means
 * exposure always takes two deliberate decisions rather than one.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LiveClassPublicationService {

    private final LiveClassRepository liveClassRepository;
    private final AuditService auditService;

    /**
     * Teacher action: hand a class to Platform Admin for review.
     *
     * <p>Only the owning teacher may submit, and only from DRAFT or REJECTED. A teacher cannot
     * approve or publish their own class, which would make the review step decorative.</p>
     */
    @Transactional
    public LiveClass submitForApproval(UUID liveClassId, UUID userId, String actorEmail) {
        LiveClass liveClass = require(liveClassId);

        // The caller's userId must match the class's teacher's userId
        if (!liveClass.getTeacher().getUserId().equals(userId)) {
            throw new ForbiddenException("live class", "submit for review");
        }

        String current = liveClass.getPublicationStatus();
        if (!LiveClass.PublicationStatus.DRAFT.name().equals(current)
                && !LiveClass.PublicationStatus.REJECTED.name().equals(current)) {
            throw new IllegalStateException(
                    "Only a draft or rejected class can be submitted for review (this one is " + current + ")");
        }
        if (!LiveClass.LiveClassStatus.SCHEDULED.name().equals(liveClass.getStatus())) {
            throw new IllegalStateException(
                    "Only a scheduled class can be submitted for review");
        }
        if (liveClass.getTitle() == null || liveClass.getTitle().isBlank()) {
            throw new IllegalArgumentException("A class needs a title before it can be submitted");
        }

        Map<String, Object> before = snapshot(liveClass);
        liveClass.setPublicationStatus(LiveClass.PublicationStatus.PENDING_APPROVAL.name());
        liveClass.setSubmittedForReviewAt(LocalDateTime.now());
        liveClass.setReviewedBy(null);
        liveClass.setReviewedAt(null);
        liveClass.setReviewNote(null);

        LiveClass saved = liveClassRepository.save(liveClass);
        audit(saved, actorEmail, AuditLog.AuditAction.UPDATE, before, "Submitted for public review");
        log.info("Live class {} submitted for review by {}", saved.getId(), actorEmail);
        return saved;
    }

    /** Platform Admin: accept a class as fit to announce. Does not make it public on its own. */
    @Transactional
    public LiveClass approve(UUID liveClassId, String actorEmail, String note) {
        LiveClass liveClass = requirePending(liveClassId, "approved");
        Map<String, Object> before = snapshot(liveClass);

        liveClass.setPublicationStatus(LiveClass.PublicationStatus.APPROVED.name());
        liveClass.setReviewedBy(actorEmail);
        liveClass.setReviewedAt(LocalDateTime.now());
        liveClass.setReviewNote(truncate(note));

        LiveClass saved = liveClassRepository.save(liveClass);
        audit(saved, actorEmail, AuditLog.AuditAction.UPDATE, before, "Approved for publication");
        log.info("Live class {} approved by {}", saved.getId(), actorEmail);
        return saved;
    }

    /** Platform Admin: refuse, with a note the teacher can act on. */
    @Transactional
    public LiveClass reject(UUID liveClassId, String actorEmail, String note) {
        LiveClass liveClass = requirePending(liveClassId, "rejected");
        if (note == null || note.isBlank()) {
            // Without a reason the teacher cannot fix anything, and an unexplained rejection is
            // indistinguishable from a bug to the person receiving it.
            throw new IllegalArgumentException("A rejection needs a note explaining why");
        }
        Map<String, Object> before = snapshot(liveClass);

        liveClass.setPublicationStatus(LiveClass.PublicationStatus.REJECTED.name());
        liveClass.setReviewedBy(actorEmail);
        liveClass.setReviewedAt(LocalDateTime.now());
        liveClass.setReviewNote(truncate(note));

        LiveClass saved = liveClassRepository.save(liveClass);
        audit(saved, actorEmail, AuditLog.AuditAction.UPDATE, before, "Rejected from public listing");
        log.info("Live class {} rejected by {}", saved.getId(), actorEmail);
        return saved;
    }

    /**
     * Platform Admin: make it publicly discoverable.
     *
     * <p>Refuses to publish a class whose access level is not PUBLIC. Publishing something that
     * is still institution-scoped would be meaningless at best and misleading at worst, and a
     * single publish action silently flipping an INSTITUTION class onto the marketing site is
     * exactly the failure mode this feature must not have.</p>
     */
    @Transactional
    public LiveClass publish(UUID liveClassId, String actorEmail) {
        LiveClass liveClass = require(liveClassId);
        String current = liveClass.getPublicationStatus();

        if (!LiveClass.PublicationStatus.APPROVED.name().equals(current)
                && !LiveClass.PublicationStatus.PUBLISHED.name().equals(current)) {
            throw new IllegalStateException(
                    "Only an approved class can be published (this one is " + current + ")");
        }
        if (!LiveClass.AccessLevel.PUBLIC.name().equals(liveClass.getAccessLevel())) {
            throw new IllegalStateException(
                    "This class is scoped to " + liveClass.getAccessLevel()
                            + ", so it cannot be published publicly. Set its access level to PUBLIC first.");
        }
        if (LiveClass.LiveClassStatus.CANCELLED.name().equals(liveClass.getStatus())) {
            throw new IllegalStateException("A cancelled class cannot be published");
        }

        Map<String, Object> before = snapshot(liveClass);
        liveClass.setPublicationStatus(LiveClass.PublicationStatus.PUBLISHED.name());
        liveClass.setPublishedAt(LocalDateTime.now());
        liveClass.setReviewedBy(actorEmail);
        liveClass.setReviewedAt(LocalDateTime.now());

        LiveClass saved = liveClassRepository.save(liveClass);
        audit(saved, actorEmail, AuditLog.AuditAction.PUBLISH, before, "Published to public listing");
        log.info("Live class {} published by {}", saved.getId(), actorEmail);
        return saved;
    }

    /**
     * Platform Admin: withdraw from the public listing.
     *
     * <p>Soft: the class keeps its attendance, recordings, teacher ownership and audit history.
     * A cancelled session or an ineligible class leaves the marketing site without any academic
     * record being touched.</p>
     */
    @Transactional
    public LiveClass unpublish(UUID liveClassId, String actorEmail, String reason) {
        LiveClass liveClass = require(liveClassId);
        if (!LiveClass.PublicationStatus.PUBLISHED.name().equals(liveClass.getPublicationStatus())) {
            throw new IllegalStateException("Only a published class can be unpublished");
        }
        Map<String, Object> before = snapshot(liveClass);

        liveClass.setPublicationStatus(LiveClass.PublicationStatus.APPROVED.name());
        liveClass.setPublishedAt(null);
        liveClass.setReviewedBy(actorEmail);
        liveClass.setReviewedAt(LocalDateTime.now());
        liveClass.setReviewNote(truncate(reason));

        LiveClass saved = liveClassRepository.save(liveClass);
        audit(saved, actorEmail, AuditLog.AuditAction.UNPUBLISH, before, "Removed from public listing");
        log.info("Live class {} unpublished by {}", saved.getId(), actorEmail);
        return saved;
    }

    /**
     * Platform Admin: archive a finished class out of the working set.
     *
     * <p>Soft delete. Attendance records, teacher ownership, recordings and audit history are all
     * preserved deliberately - the brief is explicit that ending a session must not destroy
     * academic or operational records, and there is no retention policy in this codebase that
     * would justify a physical delete.</p>
     */
    @Transactional
    public void archive(UUID liveClassId, String actorEmail, String reason) {
        LiveClass liveClass = require(liveClassId);
        if (LiveClass.LiveClassStatus.IN_PROGRESS.name().equals(liveClass.getStatus())
                || LiveClass.LiveClassStatus.LIVE.name().equals(liveClass.getStatus())
                || LiveClass.LiveClassStatus.STARTING.name().equals(liveClass.getStatus())) {
            // Archiving a running session would orphan the LiveKit room and strand participants.
            throw new IllegalStateException("End the session before archiving it");
        }
        Map<String, Object> before = snapshot(liveClass);

        liveClass.setPublicationStatus(LiveClass.PublicationStatus.DRAFT.name());
        liveClass.setPublishedAt(null);
        liveClass.setIsDeleted(true);

        LiveClass saved = liveClassRepository.save(liveClass);
        audit(saved, actorEmail, AuditLog.AuditAction.ARCHIVE, before,
                "Archived" + (reason == null || reason.isBlank() ? "" : ": " + reason));
        log.info("Live class {} archived by {}", saved.getId(), actorEmail);
    }

    @Transactional(readOnly = true)
    public Page<LiveClass> reviewQueue(String publicationStatus, String accessLevel, int page, int size) {
        int bounded = Math.max(1, Math.min(size, 100));
        return liveClassRepository.findForPublicationReview(
                blankToNull(publicationStatus), blankToNull(accessLevel),
                PageRequest.of(Math.max(0, page), bounded));
    }

    @Transactional(readOnly = true)
    public LiveClass get(UUID id) {
        return require(id);
    }

    private LiveClass requirePending(UUID id, String action) {
        LiveClass liveClass = require(id);
        if (!LiveClass.PublicationStatus.PENDING_APPROVAL.name().equals(liveClass.getPublicationStatus())) {
            throw new IllegalStateException(
                    "Only a class pending approval can be " + action + " (this one is "
                            + liveClass.getPublicationStatus() + ")");
        }
        return liveClass;
    }

    private LiveClass require(UUID id) {
        return liveClassRepository.findById(id)
                .filter(lc -> !Boolean.TRUE.equals(lc.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("LiveClass", "id", id));
    }

    private Map<String, Object> snapshot(LiveClass lc) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("publicationStatus", lc.getPublicationStatus());
        values.put("accessLevel", lc.getAccessLevel());
        values.put("status", lc.getStatus());
        return values;
    }

    /** Audit write that can never fail the business transaction. */
    private void audit(LiveClass lc, String actorEmail, AuditLog.AuditAction action,
                       Map<String, Object> before, String description) {
        try {
            Map<String, Object> after = new LinkedHashMap<>(snapshot(lc));
            after.put("description", description);
            auditService.recordAuditLog(
                    lc.getInstitutionId(), null, actorEmail, "ADMIN",
                    "LIVE_CLASS", lc.getId(), lc.getTitle(),
                    action, before, after);
        } catch (Exception ex) {
            log.warn("Audit write failed for LIVE_CLASS {} {}: {}", action, lc.getId(), ex.getMessage());
        }
    }

    private static String truncate(String note) {
        if (note == null || note.isBlank()) {
            return null;
        }
        return note.length() > 500 ? note.substring(0, 500) : note;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}