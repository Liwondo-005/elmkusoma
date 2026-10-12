package tz.elmkusoma.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tz.elmkusoma.course.domain.LiveClass;

import java.time.LocalDateTime;

/**
 * A live class as Platform Admin sees it for publication governance.
 *
 * <p>Unlike {@link PublicLiveClassView} this may carry internal identifiers, because it is only
 * ever returned to an authenticated administrator. It still does not carry {@code recordingUrl}:
 * recording access is entitlement-checked at the point of use, and an admin listing is not that
 * point.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveClassGovernanceView {

    private String id;

    private String title;

    private String description;

    private String status;

    private String publicationStatus;

    private String accessLevel;

    private String coverImageUrl;

    private String teacherId;

    private String institutionId;

    private LocalDateTime scheduledAt;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    private Integer durationMinutes;

    private Integer maxParticipants;

    private LocalDateTime submittedForReviewAt;

    private LocalDateTime publishedAt;

    private String reviewedBy;

    private LocalDateTime reviewedAt;

    private String reviewNote;

    private LocalDateTime updatedAt;

    /**
     * Whether this class is reachable by an anonymous visitor right now.
     *
     * <p>Derived server-side so the admin screen shows the same truth the public query enforces,
     * rather than an optimistic "published" label on a class that is still institution-scoped.</p>
     */
    private boolean publiclyDiscoverable;

    public static LiveClassGovernanceView from(LiveClass lc) {
        return LiveClassGovernanceView.builder()
                .id(lc.getId().toString())
                .title(lc.getTitle())
                .description(lc.getDescription())
                .status(lc.getStatus())
                .publicationStatus(lc.getPublicationStatus())
                .accessLevel(lc.getAccessLevel())
                .coverImageUrl(lc.getCoverImageUrl())
                .teacherId(lc.getTeacherId() == null ? null : lc.getTeacherId().toString())
                .institutionId(lc.getInstitutionId() == null ? null : lc.getInstitutionId().toString())
                .scheduledAt(lc.getScheduledAt())
                .startedAt(lc.getStartedAt())
                .endedAt(lc.getEndedAt())
                .durationMinutes(lc.getDurationMinutes())
                .maxParticipants(lc.getMaxParticipants())
                .submittedForReviewAt(lc.getSubmittedForReviewAt())
                .publishedAt(lc.getPublishedAt())
                .reviewedBy(lc.getReviewedBy())
                .reviewedAt(lc.getReviewedAt())
                .reviewNote(lc.getReviewNote())
                .updatedAt(lc.getUpdatedAt())
                .publiclyDiscoverable(lc.isPubliclyDiscoverable())
                .build();
    }
}