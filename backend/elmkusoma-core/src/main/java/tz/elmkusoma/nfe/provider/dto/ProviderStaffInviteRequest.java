package tz.elmkusoma.nfe.provider.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A provider owner's request to provision a member of staff (authority level C -&gt; D).
 *
 * <p>Note what is absent: there is no {@code role} field. The endpoint grants exactly one role,
 * {@code PROVIDER_STAFF}, because a role chosen by the client is a role the server never
 * validated. Responsibility is descriptive and carries no authority - see
 * {@link tz.elmkusoma.shared.domain.InstitutionMembership#getResponsibility()}.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderStaffInviteRequest {

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    @Size(max = 120)
    private String firstName;

    @Size(max = 120)
    private String lastName;

    /**
     * Optional. Free-form, matched against {@code ^[A-Z][A-Z0-9_]{1,59}$} so it reads as a label
     * rather than prose, and so it can never carry a payload into a UI that renders it.
     */
    @Size(max = 60)
    private String responsibility;
}
