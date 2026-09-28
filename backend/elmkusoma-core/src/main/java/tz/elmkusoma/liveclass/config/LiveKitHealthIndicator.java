package tz.elmkusoma.liveclass.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import tz.elmkusoma.liveclass.service.LiveKitService;

@Component
public class LiveKitHealthIndicator implements HealthIndicator {

    private final LiveKitService liveKitService;

    public LiveKitHealthIndicator(LiveKitService liveKitService) {
        this.liveKitService = liveKitService;
    }

    @Override
    public Health health() {
        if (liveKitService.isAvailable()) {
            return Health.up()
                    .withDetail("livekit", "configured")
                    .withDetail("mode", "video+audio+chat")
                    .build();
        } else if (liveKitService.isConfigured()) {
            // Credentials exist but the signal server cannot be reached: classes
            // fall back to chat-only instead of handing out a dead ws:// URL.
            return Health.up()
                    .withDetail("livekit", "unreachable")
                    .withDetail("mode", "chat only")
                    .withDetail("message", "LiveKit server is not reachable - start it or fix LIVEKIT_URL")
                    .build();
        } else {
            return Health.up()
                    .withDetail("livekit", "not configured")
                    .withDetail("mode", "chat only")
                    .withDetail("message", "Set LIVEKIT_URL, LIVEKIT_API_KEY, LIVEKIT_API_SECRET to enable video")
                    .build();
        }
    }
}
