package tz.elmkusoma.administration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateScheduledReportRequest {

    @NotBlank(message = "title is required")
    @Size(max = 255)
    private String title;

    @NotBlank(message = "reportType is required")
    @Pattern(regexp = "PERFORMANCE|ATTENDANCE|LEARNERS|DATA_QUALITY|GOVERNANCE",
            message = "reportType must be PERFORMANCE, ATTENDANCE, LEARNERS, DATA_QUALITY or GOVERNANCE")
    private String reportType;

    @NotBlank(message = "frequency is required")
    @Pattern(regexp = "DAILY|WEEKLY|MONTHLY", message = "frequency must be DAILY, WEEKLY or MONTHLY")
    private String frequency;

    @Size(max = 2000)
    private String recipients;
}
