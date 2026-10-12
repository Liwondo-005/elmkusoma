package tz.elmkusoma.administration.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Audit B-35: grants a provider a service package.
 *
 * <p>{@code provider_service_entitlements} could be read and updated but never created, so
 * {@code GET /providers/{id}/quotas} always returned an empty list and sponsored-seat commerce
 * was unusable out of the box. This is the missing write for that existing table.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntitlementCreateRequest {

    /** Institution whose NFE provider receives the entitlement. */
    @NotNull(message = "institutionId is required")
    private UUID institutionId;

    /** Service from the existing platform_services catalogue. */
    @NotNull(message = "serviceId is required")
    private UUID serviceId;

    private Integer maxSeats;
    private LocalDateTime startsAt;
    private LocalDateTime expiresAt;
}