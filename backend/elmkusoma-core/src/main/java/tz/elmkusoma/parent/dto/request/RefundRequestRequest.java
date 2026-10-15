package tz.elmkusoma.parent.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RefundRequestRequest {

    @NotNull(message = "A refund amount is required")
    @DecimalMin(value = "0.01", message = "A refund amount must be greater than zero")
    private BigDecimal amount;

    @NotBlank(message = "A refund reason is required")
    @Size(max = 500, message = "A refund reason must be at most 500 characters")
    private String reason;

    /** Optional supporting link. Evidence review is required for large refunds by policy. */
    @Size(max = 500)
    private String evidenceUrl;
}
