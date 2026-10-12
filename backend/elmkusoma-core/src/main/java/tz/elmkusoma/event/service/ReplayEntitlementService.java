package tz.elmkusoma.event.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tz.elmkusoma.event.domain.Replay;
import tz.elmkusoma.event.repository.EventRegistrationRepository;
import tz.elmkusoma.liveclass.repository.LiveClassParticipantRepository;

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

    /**
     * A learner may open a replay of a session they were actually part of - a
     * LiveClassParticipant row for live-class replays, or an EventRegistration for event
     * replays. Institution membership alone is not enough, because that would expose every
     * recording in the institution to every student.
     *
     * <p>A replay with neither a session nor an event link has no participant evidence at
     * all, so it is denied.</p>
     */
    public boolean hasEntitlement(Replay replay, UUID userId) {
        if (replay == null || userId == null) {
            return false;
        }
        if (replay.getLiveSessionId() != null) {
            return liveClassParticipantRepository
                    .existsByLiveClassIdAndUserIdAndIsDeletedFalse(replay.getLiveSessionId(), userId);
        }
        if (replay.getEventId() != null) {
            return eventRegistrationRepository
                    .existsByEventIdAndUserIdAndIsDeletedFalse(replay.getEventId(), userId);
        }
        return false;
    }
}
