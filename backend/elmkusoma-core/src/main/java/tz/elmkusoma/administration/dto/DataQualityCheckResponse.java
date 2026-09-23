package tz.elmkusoma.administration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataQualityCheckResponse {
    private String name;
    private String status;   // OK | WARN | UNAVAILABLE
    private Long count;
    private String detail;
}
