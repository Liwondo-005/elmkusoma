package tz.elmkusoma.event.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Response for POST /v1/learner/events/{eventId}/join (§90/§99/§104).
 * Carries a short-lived LiveKit token for the event room plus the external
 * meetingUrl fallback (§59: only present for registered/organizer/teacher/admin).
 */
@Data
@Builder
public class EventJoinResponse {
    private String token;
    private String serverUrl;
    private String roomName;
    private String meetingUrl;
    private String eventStatus;
    private Boolean liveKitAvailable;
    /** True when the event is only in the waiting room (PREPARING) — not yet LIVE. */
    private Boolean waitingRoom;
}
