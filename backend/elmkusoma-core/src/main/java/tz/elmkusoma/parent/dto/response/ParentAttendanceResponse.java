package tz.elmkusoma.parent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParentAttendanceResponse {

    private String studentName;
    private String className;
    private BigDecimal attendancePercentage;
    private Long totalDays;
    private Long presentDays;
    private Long absentDays;
    private Long lateDays;
    private Long excusedDays;
    private List<AttendanceDay> recentDays;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttendanceDay {
        private LocalDate date;
        private String status;
        private String remarks;
    }
}
