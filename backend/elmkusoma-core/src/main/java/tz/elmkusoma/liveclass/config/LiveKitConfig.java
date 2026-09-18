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
    private IngressConfig ingress = new IngressConfig();
    private EgressConfig egress = new EgressConfig();

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

    @Data
    public static class IngressConfig {
        private boolean enabled = false;
        private String whipEndpoint = "";
        private String rtmpEndpoint = "";
        private String srtEndpoint = "";
    }

    @Data
    public static class EgressConfig {
        private boolean enabled = false;
        private String outputBucket = "";
        private String outputPath = "recordings/";
    }

    public boolean isConfigured() {
        return server.getApiKey() != null && !server.getApiKey().isEmpty()
                && server.getApiSecret() != null && !server.getApiSecret().isEmpty();
    }

    public boolean isIngressEnabled() {
        return isConfigured() && ingress.isEnabled();
    }

    public boolean isEgressEnabled() {
        return isConfigured() && egress.isEnabled();
    }
}
