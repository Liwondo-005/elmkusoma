package tz.elmkusoma.oversight.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceResponse {
    private Double overallRate;
    private Long totalStudents;
    private Long schoolsAtRisk;
    private List<DailyTrend> dailyTrend;
    private List<SchoolAttendance> schoolAttendance;
    private List<LowAttendanceStudent> lowAttendanceStudents;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailyTrend {
        private String date;
        private Long present;
        private Long absent;
        private Long late;
        private Long excused;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SchoolAttendance {
        private String institutionId;
        private String institutionName;
        private String institutionCode;
        private Long studentCount;
        private Double attendanceRate;
        private Long absentCount;
        private Long lateCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LowAttendanceStudent {
        private String studentId;
        private String studentName;
        private String institutionName;
        private Double attendanceRate;
        private Long daysAbsent;
    }
}
