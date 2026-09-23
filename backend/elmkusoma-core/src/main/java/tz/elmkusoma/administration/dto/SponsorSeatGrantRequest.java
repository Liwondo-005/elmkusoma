package tz.elmkusoma.administration.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SponsorSeatGrantRequest {
    @NotNull
    private UUID providerId;
    @NotNull
    private UUID serviceId;
    @NotNull
    private UUID userId;
    @NotNull
    private UUID studentId;
    @NotNull
    @Min(1)
    private Integer seats = 1;
    private LocalDateTime expiresAt;
    private String note;
}
