package tz.elmkusoma.administration.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstitutionServiceRequest {
    @NotBlank
    private String featureKey;

    private Boolean enabled;

    private String configuration;
}