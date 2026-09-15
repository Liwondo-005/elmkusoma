package tz.elmkusoma.course.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "live_class_participants", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"live_class_id", "student_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class LiveClassParticipant extends BaseEntity {

    @Column(name = "live_class_id", nullable = false)
    private UUID liveClassId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @Column(name = "attendance_status", length = 20)
    private String attendanceStatus = "JOINED";

    public enum AttendanceStatus {
        JOINED, LEFT, COMPLETED, ABSENT
    }
}
