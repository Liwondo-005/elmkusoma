package tz.elmkusoma.administration.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class SponsorGrantResponse {
    private int seatsGranted;
    private int seatsUsed;
    private Integer maxSeats;
    private List<UUID> entitlementIds;
}
