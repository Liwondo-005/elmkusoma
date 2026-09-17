package tz.elmkusoma.nfe.attendance.dto;

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
public class AttendanceResponse {

    private UUID id;
    private UUID institutionId;
    private UUID providerId;
    private UUID sessionId;
    private UUID learnerId;
    private String status;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private String remarks;
    private UUID markedBy;
    private LocalDateTime createdAt;
}
