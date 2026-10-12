package tz.elmkusoma.liveclass.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Builds the <em>application-owned</em> recording playback URLs that are handed to clients.
 *
 * <p>Before this class existed the API returned the raw storage location straight to the
 * browser, and every consumer ({@code <video src>}, {@code <a href>}, {@code window.open})
 * pointed the browser at object storage directly. That made the JSON endpoints the only
 * place authorisation was ever checked: once a storage URL had been seen it could be copied,
 * replayed and shared forever, and nothing on the byte path re-checked entitlement.</p>
 *
 * <p>The URLs produced here are same-origin paths on the frontend base URL, so the browser
 * attaches the application's own access-token cookie exactly as it already does for every
 * other same-origin request. Each byte fetch is therefore an authenticated request that the
 * playback endpoints authorise on every Range request, rather than a bearer capability.</p>
 *
 * <p>The base URL is {@code app.frontend-url}, which is mapped from {@code FRONTEND_URL} and
 * validated at startup by {@link tz.elmkusoma.config.FrontendUrlValidator}. Keeping the two
 * on the same property means a misconfigured frontend URL can never silently point playback
 * at a host the frontend does not own.</p>
 */
@Component
public class RecordingPlaybackUrls {

    private final String frontendUrl;

    public RecordingPlaybackUrls(@Value("${app.frontend-url:http://localhost:3000}") String frontendUrl) {
        String trimmed = frontendUrl == null ? "" : frontendUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        this.frontendUrl = trimmed;
    }

    /** Playback URL for a live-class recording, keyed by the trusted live-class identifier. */
    public String forLiveClass(UUID liveClassId) {
        if (frontendUrl.isEmpty() || liveClassId == null) {
            return null;
        }
        return frontendUrl + "/v1/live-session/classes/" + liveClassId + "/recording/playback";
    }

    /** Playback URL for a replay, keyed by the trusted replay identifier. */
    public String forReplay(UUID replayId) {
        if (frontendUrl.isEmpty() || replayId == null) {
            return null;
        }
        return frontendUrl + "/v1/replays/" + replayId + "/playback";
    }
}
