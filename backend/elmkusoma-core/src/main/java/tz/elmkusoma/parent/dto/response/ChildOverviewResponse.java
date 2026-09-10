package tz.elmkusoma.parent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChildOverviewResponse {

    private UUID studentId;
    private String studentName;
    private String admissionNumber;
    private String relationshipType;
    private Boolean isPrimary;
    private String className;
    private String educationLevel;

    private Long totalDays;
    private Long daysPresent;
    private Long daysAbsent;
    private Long daysLate;
    private Double attendancePercentage;

    private Long totalAssignments;
    private Long completedAssignments;
    private Long overdueAssignments;
    private Long pendingAssignments;

    private String latestGrade;
    private Double latestAverage;
    private Integer classRank;
    private Integer totalStudentsInClass;

    private Double learningProgress;
}
