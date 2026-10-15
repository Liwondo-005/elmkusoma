package tz.elmkusoma.payments.gateway;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Normalises Tanzanian phone numbers to the MSISDN format Stakaba requires.
 *
 * <p>Per {@code https://docs.stakaba.com/get-started/overview.md}, every number
 * must arrive as {@code 255XXXXXXXXX}: {@code 0742 000 331}, {@code +255742000331}
 * and {@code 742000331} all normalise to {@code 255742000331}. The final form
 * must be the country code 255 followed by a valid Tanzanian mobile prefix
 * (6x or 7x) and eight further digits.</p>
 */
public final class MsisdnNormalizer {

    /** 255 + mobile prefix (6x/7x) + 8 digits, per the documented format. */
    private static final Pattern MSISDN = Pattern.compile("^255[67]\\d{8}$");

    private MsisdnNormalizer() {
    }

    /**
     * Normalises {@code raw} to {@code 255XXXXXXXXX}.
     *
     * @throws IllegalArgumentException when the value cannot be a Tanzanian
     *         mobile number in any of the documented input forms
     */
    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Mobile number is required");
        }
        String cleaned = raw.trim().replace(" ", "").replace("-", "")
                .replace("(", "").replace(")", "");
        if (cleaned.startsWith("+")) {
            cleaned = cleaned.substring(1);
        }
        if (cleaned.startsWith("0")) {
            cleaned = "255" + cleaned.substring(1);
        } else if (cleaned.startsWith("255")) {
            // already country-coded
        } else if (cleaned.length() == 9) {
            cleaned = "255" + cleaned;
        } else {
            throw new IllegalArgumentException(
                    "Invalid mobile number. Use format 255XXXXXXXXX (e.g. 255742000331)");
        }
        cleaned = cleaned.toLowerCase(Locale.ROOT);
        if (!MSISDN.matcher(cleaned).matches()) {
            throw new IllegalArgumentException(
                    "Invalid mobile number. Use format 255XXXXXXXXX (e.g. 255742000331)");
        }
        return cleaned;
    }

    /** True when {@code raw} normalises without error. Handy for UI-side pre-checks. */
    public static boolean isValid(String raw) {
        try {
            normalize(raw);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
