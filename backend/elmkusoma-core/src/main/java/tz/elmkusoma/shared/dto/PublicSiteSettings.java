package tz.elmkusoma.shared.dto;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Public, non-secret site configuration as consumed by the landing page, footer, contact page
 * and support page.
 *
 * <p>Only keys on {@link #APPROVED_KEYS} are ever returned, and only when they resolve to a
 * non-blank value. A blank value is omitted rather than returned as {@code ""}, so the client
 * can hide an unset phone number or social link without treating "empty" as a real setting.</p>
 *
 * <p>{@link #validationError} is the single source of truth for what a public value may contain,
 * and it is enforced on <em>both</em> paths:</p>
 * <ul>
 *   <li>the read path drops a value that fails it, so a row edited directly in the database or
 *       seeded by a migration can never push a {@code javascript:} URL into a browser;</li>
 *   <li>the admin write path rejects it with 400, so an administrator is told their save failed
 *       instead of receiving "updated" and then finding the value silently missing from the
 *       public site.</li>
 * </ul>
 *
 * <p>Read-side filtering alone was not enough: the public endpoint dropped an invalid value
 * silently, so a saved-but-rejected URL looked identical to a successful one. That is the defect
 * this class now prevents from both ends.</p>
 */
public final class PublicSiteSettings {

    /**
     * Explicit allow-list.
     *
     * <p>Filtering on {@code is_public} alone would be trusting a database flag to be the only
     * thing standing between the public internet and an internal recipient list. The allow-list
     * means a row someone marks public by mistake still cannot reach an anonymous caller, and a
     * key added to the table later is not exposed until it is reviewed and listed here.</p>
     */
    public static final List<String> APPROVED_KEYS = List.of(
            "public.contact.email",
            "public.contact.supportEmail",
            "public.contact.phone",
            "public.contact.whatsapp",
            "public.contact.address",
            "public.contact.workingHours",
            "public.contact.responseTime",
            "public.social.facebook",
            "public.social.instagram",
            "public.social.youtube",
            "public.social.linkedin",
            "public.social.tiktok",
            "public.social.x",
            // Headquarters. The campus-level text keys are the only ones seeded with a value;
            // the precise-address and map keys stay unset until the owner verifies them, and an
            // unset key is omitted rather than rendered as a dead "View on Map" button.
            "public.location.name",
            "public.location.campus",
            "public.location.city",
            "public.location.country",
            "public.location.building",
            "public.location.address",
            "public.location.mapUrl",
            "public.location.directionsUrl",
            "public.location.description",
            "public.location.hours"
    );

    /** Only these schemes may reach a browser. Explicitly not javascript:, data:, vbscript:. */
    private static final Pattern SAFE_URL = Pattern.compile("^https?://[\\x20-\\x7E]+$");

    /** International format for a dialable number: leading + then digits, spaces and dashes. */
    private static final Pattern SAFE_PHONE = Pattern.compile("^\\+[0-9 ()-]{6,24}$");

    /** Click-to-chat needs bare digits with country code and nothing else. */
    private static final Pattern WHATSAPP_DIGITS = Pattern.compile("^[0-9]{8,15}$");

    private static final Pattern DELIVERABLE_ADDRESS =
            Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[A-Za-z]{2,}$");

    /** Upper bound for free-text settings rendered as text nodes. */
    private static final int MAX_FREE_TEXT = 500;

    private PublicSiteSettings() {
    }

    public static boolean isApproved(String key) {
        return key != null && APPROVED_KEYS.contains(key);
    }

    /**
     * The reason a value may not be stored, or {@code null} when it is acceptable.
     *
     * <p>A blank value is always acceptable and means "clear this setting". That is deliberate:
     * an administrator must be able to unset a phone number or an unverified map link, and the
     * public endpoint already omits blanks.</p>
     *
     * <p>Keys outside {@link #APPROVED_KEYS} return {@code null}. They are internal settings
     * (SMTP hosts, retention windows, feature flags) whose formats this class has no business
     * second-guessing, and constraining them here would break unrelated administration.</p>
     */
    public static String validationError(String key, String value) {
        if (key == null || !isApproved(key)) {
            return null;
        }
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();

        String lower = key.toLowerCase(Locale.ROOT);
        if (lower.startsWith("public.social.") || lower.endsWith("mapurl") || lower.endsWith("directionsurl")) {
            return SAFE_URL.matcher(trimmed).matches() ? null
                    : "must be an http:// or https:// URL";
        }
        if ("public.contact.whatsapp".equals(key)) {
            return WHATSAPP_DIGITS.matcher(trimmed).matches() ? null
                    : "must be digits only, including the country code (for example 255700000000)";
        }
        if ("public.contact.phone".equals(key)) {
            return SAFE_PHONE.matcher(trimmed).matches() ? null
                    : "must be in international format, for example +255 700 000 000";
        }
        if ("public.contact.email".equals(key) || "public.contact.supportEmail".equals(key)) {
            return DELIVERABLE_ADDRESS.matcher(trimmed).matches() ? null
                    : "must be a valid email address";
        }
        return trimmed.length() <= MAX_FREE_TEXT ? null
                : "must be " + MAX_FREE_TEXT + " characters or fewer";
    }
}
