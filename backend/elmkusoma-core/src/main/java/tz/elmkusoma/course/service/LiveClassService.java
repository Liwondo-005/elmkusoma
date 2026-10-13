package tz.elmkusoma.course.service;

import tz.elmkusoma.course.dto.CreateLiveClassRequest;
import tz.elmkusoma.course.dto.LiveClassResponse;

import java.util.List;
import java.util.UUID;

public interface LiveClassService {

    List<LiveClassResponse> getTeacherLiveClasses(UUID teacherId);

    LiveClassResponse createLiveClass(UUID teacherId, UUID institutionId, CreateLiveClassRequest request);

    LiveClassResponse updateLiveClass(UUID teacherId, UUID liveClassId, CreateLiveClassRequest request);

    void cancelLiveClass(UUID teacherId, UUID liveClassId);

    LiveClassResponse startSession(UUID teacherId, UUID liveClassId);

    LiveClassResponse endSession(UUID teacherId, UUID liveClassId, UUID markedBy);

    /**
     * Administrative force-end: accepts any running status (STARTING/LIVE/IN_PROGRESS/
     * RECOVERING/ENDING) and authorises on the acting admin's institution instead of
     * impersonating the class teacher, so attendance and certificates are attributed to
     * the admin who actually performed the action.
     */
    LiveClassResponse forceEndSession(UUID liveClassId, UUID institutionId, UUID actingUserId);

    /**
     * Authoritative expiry sweep: ends every started class whose scheduled start plus
     * configured duration has already elapsed, finalising attendance, certificates and
     * the recording/replay chain exactly like a manual end (no client, teacher or
     * refresh involved). Returns the number of sessions ended.
     */
    int endExpiredSessions();

    /**
     * Retries finalization for recordings whose storage location was still unresolved when the
     * class ended. The upload to object storage routinely outlasts the in-request polling window,
     * and the LiveKit {@code egress_ended} webhook is only delivered when a webhook URL is
     * configured on the LiveKit side, so without this pass a recording that was written
     * successfully stays invisible. Idempotent.
     *
     * @return how many pending recordings were re-examined
     */
    int reconcilePendingRecordings();

    LiveClassResponse getLiveClassById(UUID liveClassId);

    /**
     * Link an existing authorized Lesson to this teacher's live class
     * (Lesson ↔ Live Class). Validates lesson ownership/scope and prevents
     * duplicate links to the same lesson.
     */
    LiveClassResponse linkLesson(UUID teacherId, UUID liveClassId, UUID lessonId);

    /** Remove the Lesson association from this teacher's live class. */
    LiveClassResponse unlinkLesson(UUID teacherId, UUID liveClassId);

    List<LiveClassResponse> getUpcomingClasses(UUID institutionId);

    List<LiveClassResponse> getLiveClassesByStatus(UUID institutionId, String status);
}
