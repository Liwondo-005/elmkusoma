package tz.elmkusoma.administration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OffboardingChecklistResponse {
    private UUID institutionId;
    private String institutionName;
    private String lifecycleStatus;
    private List<OffboardingStep> steps;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OffboardingStep {
        private String step;
        private String status;   // DONE | PENDING | UNAVAILABLE
        private String detail;
    }
}
