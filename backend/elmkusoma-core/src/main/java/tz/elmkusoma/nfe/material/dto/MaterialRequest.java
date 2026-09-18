package tz.elmkusoma.nfe.material.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialRequest {

    @NotNull(message = "Provider ID is required")
    private UUID providerId;

    private UUID programId;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotBlank(message = "Material type is required")
    private String materialType;

    private String contentUrl;

    private Integer sortOrder;

    private Boolean isFree;
}
