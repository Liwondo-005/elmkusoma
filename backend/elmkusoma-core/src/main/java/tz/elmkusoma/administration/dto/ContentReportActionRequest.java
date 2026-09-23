package tz.elmkusoma.administration.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContentReportActionRequest {
    private String action; // RESOLVED | DISMISSED | REVIEWING
    private String notes;
}
