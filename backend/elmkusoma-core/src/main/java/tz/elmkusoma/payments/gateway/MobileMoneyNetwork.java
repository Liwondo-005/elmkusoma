package tz.elmkusoma.payments.gateway;

import java.util.Locale;

/**
 * Mobile-money networks supported by Stakaba collections.
 *
 * <p>The {@code apiValue} is the exact, case-sensitive {@code network} string
 * documented by Stakaba ({@code https://docs.stakaba.com/payments/mobile-money-collections.md}):
 * {@code Mpesa}, {@code AirtelMoney}, {@code TigoPesa} (Mixx by Yas), {@code HaloPesa}.
 * Nothing else may be sent - the gateway rejects anything that does not match
 * one of these four values exactly.</p>
 */
public enum MobileMoneyNetwork {

    MPESA("Mpesa"),
    AIRTEL_MONEY("AirtelMoney"),
    TIGO_PESA("TigoPesa"),
    HALO_PESA("HaloPesa");

    private final String apiValue;

    MobileMoneyNetwork(String apiValue) {
        this.apiValue = apiValue;
    }

    /** Exact value transmitted to Stakaba in the {@code network} field. */
    public String apiValue() {
        return apiValue;
    }

    /**
     * Parses a client-supplied network name. Accepts the documented value
     * case-insensitively plus a few common aliases so the UI cannot send a
     * string Stakaba will reject, but the value that leaves this method is
     * always one of the four exact documented constants.
     *
     * @throws IllegalArgumentException when the value is not a supported network
     */
    public static MobileMoneyNetwork from(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Mobile money network is required");
        }
        String key = raw.trim().toLowerCase(Locale.ROOT).replace("-", "").replace("_", "").replace(" ", "");
        return switch (key) {
            case "mpesa", "mmpesa", "vodacommpesa", "vodomempesa" -> MPESA;
            case "airtelmoney", "airtel" -> AIRTEL_MONEY;
            case "tigopesa", "mixxbyyas", "mixx", "tigo" -> TIGO_PESA;
            case "halopesa", "halo" -> HALO_PESA;
            default -> throw new IllegalArgumentException(
                    "Unsupported mobile money network. Supported networks: "
                            + "Mpesa, AirtelMoney, TigoPesa, HaloPesa");
        };
    }
}
