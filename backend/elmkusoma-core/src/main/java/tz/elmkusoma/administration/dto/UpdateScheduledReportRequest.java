package tz.elmkusoma.administration.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateScheduledReportRequest {

    @Size(max = 255)
    private String title;

    @Pattern(regexp = "ACTIVE|PAUSED", message = "status must be ACTIVE or PAUSED")
    private String status;

    @Pattern(regexp = "DAILY|WEEKLY|MONTHLY", message = "frequency must be DAILY, WEEKLY or MONTHLY")
    private String frequency;

    @Size(max = 2000)
    private String recipients;
}
