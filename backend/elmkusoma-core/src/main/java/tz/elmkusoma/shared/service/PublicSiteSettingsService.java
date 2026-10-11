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
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Reads public site configuration from {@code platform_config}.
 *
 * <p>Validation lives here rather than only on the admin write path, because a value can also
 * arrive by direct database edit or a migration seed, and the public endpoint must not become
 * the place that starts serving a {@code javascript:} URL.</p>
 */
@Service
@RequiredArgsConstructor
public class PublicSiteSettingsService {

    private static final Logger log = LoggerFactory.getLogger(PublicSiteSettingsService.class);

    /** Only these schemes may reach a browser. Explicitly not javascript:, data:, vbscript:. */
    private static final Pattern SAFE_URL = Pattern.compile("^https?://[\\x20-\\x7E]+$");

    /** International format for a dialable number: leading + then digits, spaces and dashes. */
    private static final Pattern SAFE_PHONE = Pattern.compile("^\\+[0-9 ()-]{6,24}$");

    /** Click-to-chat needs bare digits with country code and nothing else. */
    private static final Pattern WHATSAPP_DIGITS = Pattern.compile("^[0-9]{8,15}$");

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
        String lower = key.toLowerCase(Locale.ROOT);
        if (lower.startsWith("public.social.")) {
            return SAFE_URL.matcher(value).matches();
        }
        if ("public.contact.whatsapp".equals(key)) {
            return WHATSAPP_DIGITS.matcher(value).matches();
        }
        if ("public.contact.phone".equals(key)) {
            return SAFE_PHONE.matcher(value).matches();
        }
        if ("public.contact.email".equals(key) || "public.contact.supportEmail".equals(key)) {
            return isDeliverableAddress(value);
        }
        // Working hours, response time and address are free text rendered as text nodes.
        return value.length() <= 500;
    }
}