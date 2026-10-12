package tz.elmkusoma.certificate.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A reviewer records one decision about one stage.
 *
 * <p>{@code reason} is optional for an approval but mandatory for a rejection, and that is enforced
 * server-side rather than by bean validation alone - an empty string is not the same as absent,
 * and "withdraw if absent" would let a blank reason through.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateDecisionRequest {

    @NotNull(message = "approved is required")
    private Boolean approved;

    private String reason;

    /** Defensive: a null here must not read as "approved". */
    public boolean isApproved() {
        return Boolean.TRUE.equals(approved);
    }
}
