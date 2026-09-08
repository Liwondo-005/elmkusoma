package tz.elmkusoma.teacher.domain;

import jakarta.persistence.*;
import lombok.*;
import tz.elmkusoma.common.BaseEntity;
import tz.elmkusoma.shared.domain.User;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "teachers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Teacher extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "employee_number", unique = true)
    private String employeeNumber;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private TeacherStatus status = TeacherStatus.ACTIVE;

    @Column(name = "specialization")
    private String specialization;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    public String getFullName() {
        if (user != null) {
            return user.getFullName();
        }
        return null;
    }
}
