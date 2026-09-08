package tz.elmkusoma.nursery.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class CreateNurseryMilestoneRequest {

    @NotNull(message = "Student ID is required")
    private UUID studentId;

    @NotBlank(message = "Category is required")
    private String category;

    @NotBlank(message = "Milestone name is required")
    @Size(max = 200)
    private String milestoneName;

    @Size(max = 1000)
    private String description;

    private Integer expectedAgeMonths;

    private LocalDate achievedDate;

    @NotBlank(message = "Status is required")
    private String status;

    private String evidenceNotes;
}