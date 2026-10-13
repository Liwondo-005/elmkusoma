package tz.elmkusoma.nfe.provider.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One row of a provider's staff roster.
 *
 * <p>Built from the membership row plus the user row, never from anything the client sent. It
 * deliberately exposes no credential material of any kind - no password hash, no OTP, no token -
 * because a staff roster is not an account-recovery surface.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderStaffResponse {

    private UUID userId;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;

    /** {@code users.role} - the global identity role, which for staff is PROVIDER_STAFF. */
    private String globalRole;

    /** {@code institution_memberships.role} - the role inside the tenant. */
    private String membershipRole;

    /** Free-form responsibility, or null when nobody has assigned one. */
    private String responsibility;

    /**
     * Whether this membership is currently active. False means removed from the provider: the
     * person keeps their platform account and any other memberships, and has lost this one.
     */
    private Boolean isActive;

    private Boolean isEmailVerified;
    private UUID institutionId;
    private UUID providerId;
    private LocalDateTime membershipCreatedAt;
    private LocalDateTime updatedAt;
}
