package tz.elmkusoma.parent.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Mobile-money details for initiating or retrying a collection against an
 * existing pending payment. The number is normalised server-side to the
 * documented {@code 255XXXXXXXXX} form; the network must be one of the four
 * values Stakaba accepts (case-insensitive input is fine, the exact
 * documented value is what leaves the server).
 */
@Data
public class MobileMoneyInitiateRequest {

    @NotBlank(message = "Mobile number is required")
    private String mobileNumber;

    @NotBlank(message = "Mobile money network is required")
    private String network;
}
