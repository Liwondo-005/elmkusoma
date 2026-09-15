package tz.elmkusoma.liveclass.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "livekit")
public class LiveKitConfig {

    private ServerConfig server = new ServerConfig();
    private RoomConfig room = new RoomConfig();

    @Data
    public static class ServerConfig {
        private String url = "ws://localhost:7880";
        private String apiKey = "";
        private String apiSecret = "";
    }

    @Data
    public static class RoomConfig {
        private int emptyTimeout = 300;
        private int maxParticipants = 10000;
    }

    public boolean isConfigured() {
        return server.getApiKey() != null && !server.getApiKey().isEmpty()
                && server.getApiSecret() != null && !server.getApiSecret().isEmpty();
    }
}
