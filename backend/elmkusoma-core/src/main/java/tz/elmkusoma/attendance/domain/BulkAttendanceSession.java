package tz.elmkusoma.attendance.domain;

import jakarta.persistence.*;
import lombok.*;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "bulk_attendance_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkAttendanceSession extends BaseEntity {

    @Column(name = "class_group_id", nullable = false)
    private UUID classGroupId;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "marked_by", nullable = false)
    private UUID markedBy;

    @Column(name = "total_students", nullable = false)
    private Integer totalStudents;

    @Column(name = "marked_count", nullable = false)
    private Integer markedCount = 0;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private SessionStatus status = SessionStatus.IN_PROGRESS;

    public enum SessionStatus {
        IN_PROGRESS,
        COMPLETED,
        CANCELLED
    }
}