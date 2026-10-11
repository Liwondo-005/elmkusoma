package tz.elmkusoma.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * A live class as an anonymous visitor sees it.
 *
 * <p>Deliberately built from the entity rather than from {@code LiveClassResponse}. That response
 * populates {@code recordingUrl} verbatim from the entity, so reusing it here would publish raw
 * recording URLs to anyone who opened the marketing site. Recording access is entitlement-checked
 * server side and must stay that way - this DTO exposes {@code hasRecording} as a boolean and
 * nothing more, following the precedent in {@code StudentLiveClassController}.</p>
 *
 * <p>Also absent: {@code teacherId}, {@code institutionId}, {@code classGroupId} and any internal
 * identifier. Nothing here is needed to render a card, and internal ids are exactly what a public
 * endpoint should not hand out.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicLiveClassView {

    private String id;

    private String title;

    private String description;

    private String subjectName;

    /** Display name only, and only where the institution permits it. Null rather than a guess. */
    private String teacherName;

    private String institutionName;

    private String coverImageUrl;

    /**
     * Naive local time, as stored. Paired with {@link #timezone} so the client can render it in the
     * class's own zone rather than the visitor's browser zone.
     */
    private LocalDateTime scheduledAt;

    /** Naive local time. Null until the teacher actually starts. */
    private LocalDateTime startedAt;

    /** Null for legacy classes completed before this feature existed. */
    private LocalDateTime endedAt;

    /**
     * The class's own IANA zone, e.g. {@code Africa/Dar_es_Salaam}.
     *
     * <p>Returned so the browser does not guess. The rest of this app formats session times with
     * the visitor's ambient timezone, which shows a Dar-es-Salaam class at the wrong wall-clock
     * hour to anyone outside Tanzania.</p>
     */
    private String timezone;

    private Integer durationMinutes;

    /** Authoritative session state: SCHEDULED, IN_PROGRESS, STARTING, ENDING, COMPLETED, ENDED. */
    private String status;

    private Integer maxParticipants;

    /**
     * Whether a recording exists for public replay. NOT the URL.
     *
     * <p>Also false when a replay row exists but is still PROCESSING, so the UI never promises a
     * recording before one has been verified.</p>
     */
    private boolean hasRecording;
}