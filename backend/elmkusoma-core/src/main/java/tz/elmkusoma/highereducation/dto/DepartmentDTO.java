package tz.elmkusoma.highereducation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentDTO {

    private UUID id;

    @NotBlank(message = "Department name is required")
    private String name;

    private String code;

    private String description;

    @Builder.Default
    private Set<UUID> programmeIds = new HashSet<>();

    private UUID headOfDepartmentId;

    @Builder.Default
    private Boolean isActive = true;
}
