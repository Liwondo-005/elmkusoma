package tz.elmkusoma.parent.domain;

import jakarta.persistence.*;
import lombok.*;
import tz.elmkusoma.common.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "parent_notification_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParentNotificationPreference extends BaseEntity {

    @Column(name = "parent_id", nullable = false, unique = true)
    private UUID parentId;

    @Column(name = "attendance_alerts", nullable = false)
    private Boolean attendanceAlerts = true;

    @Column(name = "grade_alerts", nullable = false)
    private Boolean gradeAlerts = true;

    @Column(name = "fee_alerts", nullable = false)
    private Boolean feeAlerts = true;

    @Column(name = "general_announcements", nullable = false)
    private Boolean generalAnnouncements = true;

    @Column(name = "sms_enabled", nullable = false)
    private Boolean smsEnabled = false;

    @Column(name = "email_enabled", nullable = false)
    private Boolean emailEnabled = true;

    @Column(name = "push_enabled", nullable = false)
    private Boolean pushEnabled = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", insertable = false, updatable = false)
    private Parent parent;
}
