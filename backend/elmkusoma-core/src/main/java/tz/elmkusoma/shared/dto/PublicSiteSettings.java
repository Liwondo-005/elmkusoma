package tz.elmkusoma.shared.dto;

/**
 * Public, non-secret site configuration as consumed by the landing page, footer, contact page
 * and support page.
 *
 * <p>Only keys on {@link #APPROVED_KEYS} are ever returned, and only when they resolve to a
 * non-blank value. A blank value is omitted rather than returned as {@code ""}, so the client
 * can hide an unset phone number or social link without treating "empty" as a real setting.</p>
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
    public static final java.util.List<String> APPROVED_KEYS = java.util.List.of(
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
            "public.social.x"
    );

    private PublicSiteSettings() {
    }

    public static boolean isApproved(String key) {
        return key != null && APPROVED_KEYS.contains(key);
    }
}