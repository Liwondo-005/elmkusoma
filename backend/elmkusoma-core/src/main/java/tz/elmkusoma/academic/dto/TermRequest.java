package tz.elmkusoma.academic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TermRequest {

    @NotNull(message = "Institution ID is required")
    private UUID institutionId;

    @NotBlank(message = "Term name is required")
    private String name;

    @NotNull(message = "Term number is required")
    private Integer termNumber;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;
}
