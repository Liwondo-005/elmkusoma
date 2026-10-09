package tz.elmkusoma.liveclass.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
// The computed helpers below (isConfigured/isIngressEnabled/isEgressEnabled) expose JavaBean
// read-only properties that have no matching configuration key. Without ignoreInvalidFields a
// stray value bound to one of them aborts the whole application context.
@ConfigurationProperties(prefix = "livekit", ignoreInvalidFields = true)
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
        private String accessKey = "";
        private String secret = "";
        private String region = "";
        private String endpoint = "";

        /**
         * LiveKit egress requires an explicit storage destination (S3/GCP/Azure oneof).
         * Without real credentials a start would be accepted but always fail at upload,
         * so recording is treated as "not configured" until a bucket + key + secret exist.
         */
        public boolean hasStorage() {
            boolean haveCreds = outputBucket != null && !outputBucket.isEmpty()
                    && accessKey != null && !accessKey.isEmpty()
                    && secret != null && !secret.isEmpty();
            boolean haveRegion = (region != null && !region.isEmpty())
                    || (endpoint != null && !endpoint.isEmpty());
            return haveCreds && haveRegion;
        }
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

    /**
     * Setter for the computed {@link #isEgressEnabled()} property.
     *
     * <p>{@code LIVEKIT_EGRESS_ENABLED} was previously unusable: Spring relaxed-binds it to the
     * top-level {@code egressEnabled} property, found only the getter above, and aborted startup
     * with "No setter found for property: egress-enabled". That made it impossible to enable
     * recording at all - the flag had to stay unset, so every recording start was refused.</p>
     *
     * <p>This setter forwards to the real nested config, so both {@code livekit.egress-enabled}
     * and {@code livekit.egress.enabled} now work and mean the same thing. The read side stays
     * {@link #isEgressEnabled()}, which additionally requires API credentials.</p>
     */
    public void setEgressEnabled(boolean enabled) {
        egress.setEnabled(enabled);
    }

    /** Same fix as {@link #setEgressEnabled(boolean)} for the ingress flag. */
    public void setIngressEnabled(boolean enabled) {
        ingress.setEnabled(enabled);
    }
}
