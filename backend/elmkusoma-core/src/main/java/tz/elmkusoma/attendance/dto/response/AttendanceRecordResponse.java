package tz.elmkusoma.attendance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceRecordResponse {
    private UUID id;
    private UUID studentId;
    private String studentName;
    private UUID classGroupId;
    private String className;
    private LocalDate attendanceDate;
    private String status;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private String markedByName;
    private String remarks;
    private LocalDateTime createdAt;
}