package tz.elmkusoma.enrollment.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class EnrollmentRequest {

    @NotNull(message = "Student ID is required")
    private UUID studentId;

    @NotNull(message = "Class group ID is required")
    private UUID classGroupId;

    @NotNull(message = "Academic year ID is required")
    private UUID academicYearId;

    /** Audit Phase 6: optional explicit status so a placement can be created PENDING for approval. */
    private tz.elmkusoma.enrollment.domain.Enrollment.EnrollmentStatus status;
}
