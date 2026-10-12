package tz.elmkusoma.liveclass.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Reports a half-configured recorder at startup, before a class is in progress.
 *
 * <p>{@code LiveKitService} already refuses the job at request time when
 * {@link LiveKitConfig.EgressConfig#hasStorage()} is false - that is the "before accepting a
 * recording job" boundary. This adds the other boundary the configuration contract asks for:
 * an operator who flipped {@code LIVEKIT_EGRESS_ENABLED} on without completing storage learns
 * it at boot, with a precise list of which settings are missing.</p>
 *
 * <p><b>This logs rather than aborts.</b> Recording storage is optional: refusing to start the
 * whole platform over it would take down every unrelated feature for the sake of one optional
 * subsystem, and would make an otherwise valid development/test configuration unbootable. The
 * failure stays inside the recording feature - jobs are refused with a 503, nothing is
 * captured, and no doomed egress is ever created.</p>
 *
 * <p>Deliberately gated on the raw {@code livekit.egress.enabled} flag rather than
 * {@link LiveKitConfig#isEgressEnabled()}: the latter also requires LiveKit API credentials,
 * so gating on it would skip the check in exactly the case where something is missing.</p>
 *
 * <p>When recording is off, this is silent. No network call is made to the storage provider;
 * only configured values are inspected, and diagnostics list setting <em>names</em> only -
 * never a credential value.</p>
 */
@Component
public class LiveKitEgressConfigValidator implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(LiveKitEgressConfigValidator.class);

    private final LiveKitConfig config;

    public LiveKitEgressConfigValidator(LiveKitConfig config) {
        this.config = config;
    }

    @Override
    public void afterPropertiesSet() {
        if (config == null || config.getEgress() == null || !config.getEgress().isEnabled()) {
            return; // recording switched off: nothing to validate, boot normally
        }

        List<String> problems = configurationProblems(config);
        if (problems.isEmpty()) {
            log.info("LiveKit recording (egress) enabled: storage destination configured "
                    + "(values not displayed)");
            return;
        }

        log.error("LIVEKIT_EGRESS_ENABLED=true, but live recording is not usable: {}. "
                        + "Recording start requests will be refused until these are set in "
                        + "backend/.env (no value is printed here). Set "
                        + "LIVEKIT_EGRESS_ENABLED=false to run without recording.",
                String.join("; ", problems));
    }

    /**
     * Everything that would stop an egress job from being created or uploaded.
     *
     * <p>Returns an empty list when recording is switched off - there is then nothing to
     * validate - and when it is fully configured.</p>
     *
     * @return the missing/invalid setting names, or an empty list when recording is unusable
     *         or intentionally off
     */
    static List<String> configurationProblems(LiveKitConfig config) {
        List<String> problems = new ArrayList<>();
        if (config == null || config.getEgress() == null || !config.getEgress().isEnabled()) {
            return problems;
        }

        LiveKitConfig.EgressConfig egress = config.getEgress();

        if (!config.isConfigured()) {
            problems.add("LIVEKIT_API_KEY and LIVEKIT_API_SECRET (livekit.server.*) are not set, "
                    + "so no egress job could be created");
        }
        if (isBlank(egress.getOutputBucket())) {
            problems.add("LIVEKIT_EGRESS_BUCKET is empty");
        }
        if (isBlank(egress.getAccessKey())) {
            problems.add("LIVEKIT_EGRESS_S3_ACCESS_KEY is empty");
        }
        if (isBlank(egress.getSecret())) {
            problems.add("LIVEKIT_EGRESS_S3_SECRET is empty");
        }
        if (isBlank(egress.getRegion()) && isBlank(egress.getEndpoint())) {
            problems.add("neither LIVEKIT_EGRESS_S3_REGION nor LIVEKIT_EGRESS_S3_ENDPOINT is set");
        }
        if (!isBlank(egress.getEndpoint()) && !isAbsoluteHttp(egress.getEndpoint())) {
            problems.add("LIVEKIT_EGRESS_S3_ENDPOINT must be an absolute http(s) URL "
                    + "(for Backblaze B2, https://s3.<region>.backblazeb2.com)");
        }
        return problems;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static boolean isAbsoluteHttp(String value) {
        String v = value.trim().toLowerCase(Locale.ROOT);
        if (!v.startsWith("http://") && !v.startsWith("https://")) {
            return false;
        }
        try {
            URI uri = new URI(value.trim());
            return uri.getHost() != null && !uri.getHost().isBlank();
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
