package tz.elmkusoma.liveclass.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveClassReportRequest {
    @NotBlank(message = "Issue type is required")
    private String issueType;

    private String description;
    private String severity;
}
