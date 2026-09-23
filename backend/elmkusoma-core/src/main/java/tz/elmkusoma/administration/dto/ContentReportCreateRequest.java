package tz.elmkusoma.administration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContentReportCreateRequest {
    @NotBlank
    private String entityType;
    @NotNull
    private UUID entityId;
    private String entityTitle;
    private UUID reporterId;
    @NotBlank
    private String reason;
    private String description;
}
