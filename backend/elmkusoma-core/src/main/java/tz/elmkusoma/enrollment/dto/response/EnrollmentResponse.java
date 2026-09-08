package tz.elmkusoma.enrollment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tz.elmkusoma.enrollment.domain.Enrollment;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentResponse {

    private UUID id;
    private UUID studentId;
    private UUID classGroupId;
    private UUID academicYearId;
    private Enrollment.EnrollmentStatus status;
    private LocalDateTime enrolledAt;
    private LocalDateTime withdrawnAt;
    private String withdrawReason;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}
