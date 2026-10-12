package tz.elmkusoma.nfe.provider.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Assigns (or clears) a staff member's responsibility inside their provider. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderStaffResponsibilityRequest {

    /** Null or blank clears the responsibility. Otherwise {@code [A-Z][A-Z0-9_]{1,59}}. */
    @Size(max = 60)
    private String responsibility;
}
