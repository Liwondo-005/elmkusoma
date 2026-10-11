package tz.elmkusoma.shared.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.administration.domain.PlatformConfigEntry;
import tz.elmkusoma.administration.repository.PlatformConfigRepository;
import tz.elmkusoma.shared.dto.PublicSiteSettings;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reads public site configuration from {@code platform_config}.
 *
 * <p>Validation lives in {@link PublicSiteSettings} rather than only on the admin write path,
 * because a value can also arrive by direct database edit or a migration seed, and the public
 * endpoint must not become the place that starts serving a {@code javascript:} URL.</p>
 *
 * <p>This filtering is a safety net, not the primary defence: the admin write path rejects an
 * unacceptable value with 400. Silently dropping it here would otherwise make a rejected save
 * look exactly like a successful one that the public site simply chose not to show.</p>
 */
@Service
@RequiredArgsConstructor
public class PublicSiteSettingsService {

    private static final Logger log = LoggerFactory.getLogger(PublicSiteSettingsService.class);

    private final PlatformConfigRepository configRepository;

    /**
     * The public settings map.
     *
     * <p>Rejects anything not on {@link PublicSiteSettings#APPROVED_KEYS}, rejects sensitive
     * rows even if they are flagged public, and drops values that fail their format check
     * rather than passing a malformed one to the browser.</p>
     */
    @Transactional(readOnly = true)
    public Map<String, String> publicSettings() {
        Map<String, String> safe = new LinkedHashMap<>();
        for (String key : PublicSiteSettings.APPROVED_KEYS) {
            String value = configRepository.findByConfigKeyAndIsDeletedFalse(key)
                    .map(PlatformConfigEntry::getConfigValue)
                    .orElse(null);
            if (value == null || value.isBlank()) {
                continue;
            }
            String trimmed = value.trim();
            if (!isSafeForKey(key, trimmed)) {
                log.warn("Rejected unsafe or malformed value for public setting {}; omitting it", key);
                continue;
            }
            safe.put(key, trimmed);
        }
        return safe;
    }

    /** Raw lookup for server-side use (notification routing). Never for a response body. */
    @Transactional(readOnly = true)
    public String rawValue(String key) {
        return configRepository.findByConfigKeyAndIsDeletedFalse(key)
                .map(PlatformConfigEntry::getConfigValue)
                .map(String::trim)
                .filter(v -> !v.isBlank())
                .orElse(null);
    }

    /**
     * Server-side check used before sending mail. Guards against a recipient address that is
     * obviously not an address at all, so a misconfiguration cannot turn an enquiry into mail
     * to something unintended.
     */
    public static boolean isDeliverableAddress(String value) {
        return value != null && value.matches("^[^@\\s]+@[^@\\s]+\\.[A-Za-z]{2,}$");
    }

    private static boolean isSafeForKey(String key, String value) {
        return PublicSiteSettings.validationError(key, value) == null;
    }
}