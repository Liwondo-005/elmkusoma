package tz.elmkusoma.highereducation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModuleProgressRequest {

    @NotNull(message = "Progress percent is required")
    @Min(value = 0, message = "Progress percent must be between 0 and 100")
    @Max(value = 100, message = "Progress percent must be between 0 and 100")
    private Integer progressPercent;

    private String status;
}
