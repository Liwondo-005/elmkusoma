package tz.elmkusoma.attendance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceSummaryResponse {
    private UUID id;
    private UUID studentId;
    private String studentName;
    private UUID classGroupId;
    private String className;
    private UUID academicYearId;
    private UUID termId;
    private String termName;
    private Integer totalSchoolDays;
    private Integer daysPresent;
    private Integer daysAbsent;
    private Integer daysLate;
    private Integer daysExcused;
    private BigDecimal attendancePercentage;
}