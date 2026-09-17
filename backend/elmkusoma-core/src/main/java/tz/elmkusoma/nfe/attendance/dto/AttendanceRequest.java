package tz.elmkusoma.nfe.attendance.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRequest {

    private UUID providerId;

    private UUID sessionId;

    private UUID learnerId;

    @NotBlank(message = "Attendance status is required")
    private String status;

    private LocalDateTime checkInTime;

    private LocalDateTime checkOutTime;

    private String remarks;

    private UUID markedBy;
}
