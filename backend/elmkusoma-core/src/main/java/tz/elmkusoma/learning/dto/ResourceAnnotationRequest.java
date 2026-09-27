package tz.elmkusoma.learning.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceAnnotationRequest {

    @NotBlank(message = "Content must not be blank")
    @Size(max = 5000, message = "Content must be at most 5000 characters")
    private String content;

    private String positionData;

    private Boolean isPrivate;

    private UUID parentAnnotationId;
}
