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
        } else {
            return Health.up()
                    .withDetail("livekit", "not configured")
                    .withDetail("mode", "chat only")
                    .withDetail("message", "Set LIVEKIT_URL, LIVEKIT_API_KEY, LIVEKIT_API_SECRET to enable video")
                    .build();
        }
    }
}
