package tz.elmkusoma.administration.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntitlementUpdateRequest {
    private Integer maxSeats;
    private String status;
}
