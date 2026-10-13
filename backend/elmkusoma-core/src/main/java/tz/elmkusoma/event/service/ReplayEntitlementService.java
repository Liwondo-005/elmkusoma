package tz.elmkusoma.event.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tz.elmkusoma.course.domain.LiveClass;
import tz.elmkusoma.event.domain.Replay;
import tz.elmkusoma.event.repository.EventRegistrationRepository;
import tz.elmkusoma.liveclass.repository.LiveClassParticipantRepository;
import tz.elmkusoma.liveclass.service.LiveClassRecordingAccessService;

import java.util.UUID;

/**
 * The single definition of who may open a replay.
 *
 * <p>This logic used to live as a private method on {@code LearnerReplayController}. With a
 * recording byte path now authorised per request, the identical rule is needed on the
 * playback endpoint too, and two copies of an entitlement rule is exactly how one of them
 * drifts while the other still looks correct. Both callers delegate here.</p>
 */
@Service
@RequiredArgsConstructor
public class ReplayEntitlementService {

    private final LiveClassParticipantRepository liveClassParticipantRepository;
    private final EventRegistrationRepository eventRegistrationRepository;
    private final LiveClassRecordingAccessService liveClassRecordingAccessService;

    /**
     * A learner may open a live-class replay when they were <em>part of the session</em> or when
     * they <em>belong to the audience the session was made for</em>; an event replay still needs
     * its registration, because opting into an event is how that audience is declared.
     *
     * <p>Two sufficient conditions, both server-derived and neither trusting the caller:</p>
     * <ul>
     *   <li><b>Attendance</b> - a {@link tz.elmkusoma.liveclass.domain.LiveClassParticipant} row
     *       for live-class replays. This was the whole of the old rule and is kept verbatim, so
     *       nobody who could watch a recording yesterday loses it today.</li>
     *   <li><b>Audience membership</b> - the class group the session was scheduled for, resolved
     *       through the same {@code student_class_assignments} ∪ ENROLLED {@code enrollments}
     *       union that {@code LiveSessionController} already demanded before letting the learner
     *       join the room. Absence from one session is not loss of entitlement to the class, and
     *       a learner who enrolled after the recording was made is entitled to it now.</li>
     * </ul>
     *
     * <p>Neither condition holding means denial, which preserves the rule that makes this service
     * worth having: institution membership alone is not enough, so a learner cannot reach a
     * recording from a class they have no relationship with. A replay with neither a session nor
     * an event link has no participant evidence at all, and a session whose live class is missing
     * or deleted has no audience to prove - both are denied.</p>
     */
    public boolean hasEntitlement(Replay replay, UUID userId) {
        if (replay == null || userId == null) {
            return false;
        }
        if (replay.getLiveSessionId() != null) {
            // 1. Present in the recorded session.
            if (liveClassParticipantRepository
                    .existsByLiveClassIdAndUserIdAndIsDeletedFalse(replay.getLiveSessionId(), userId)) {
                return true;
            }
            // 2. Otherwise, a member of the class the session was recorded for.
            LiveClass liveClass = liveClassRecordingAccessService.findClass(replay.getLiveSessionId());
            if (liveClass == null) {
                return false;
            }
            return liveClassRecordingAccessService.isLearnerInClassAudience(
                    userId,
                    // The tenant the caller was already scoped to for this replay, so the check can
                    // never widen past the boundary the endpoint has enforced.
                    replay.getInstitutionId(),
                    liveClass.getClassGroupId());
        }
        if (replay.getEventId() != null) {
            return eventRegistrationRepository
                    .existsByEventIdAndUserIdAndIsDeletedFalse(replay.getEventId(), userId);
        }
        return false;
    }
}
