package tz.elmkusoma.attendance.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class BulkMarkAttendanceRequest {

    @NotNull(message = "Class group ID is required")
    private UUID classGroupId;

    @NotNull(message = "Attendance date is required")
    @JsonProperty("attendanceDate")
    private LocalDate attendanceDate;

    @JsonProperty("date")
    public void setDate(LocalDate date) {
        this.attendanceDate = date;
    }

    @NotEmpty(message = "Attendance records are required")
    private List<AttendanceEntry> records;

    @Data
    public static class AttendanceEntry {
        @NotNull(message = "Student ID is required")
        private UUID studentId;

        @NotBlank(message = "Status is required")
        private String status;

        private String remarks;
    }
}