package tz.elmkusoma.attendance.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class MarkAttendanceRequest {

    @NotNull(message = "Student ID is required")
    private UUID studentId;

    @NotNull(message = "Class group ID is required")
    private UUID classGroupId;

    @NotNull(message = "Attendance date is required")
    private LocalDate attendanceDate;

    @NotBlank(message = "Status is required")
    private String status;

    private String remarks;
}