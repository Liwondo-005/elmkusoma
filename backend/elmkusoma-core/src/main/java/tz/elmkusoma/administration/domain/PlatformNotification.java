package tz.elmkusoma.administration.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "platform_notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PlatformNotification extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "notification_type", nullable = false)
    private String notificationType;

    @Column(nullable = false)
    private String priority = "NORMAL";

    @Column(name = "target_audience")
    private String targetAudience;

    @Column(name = "target_role")
    private String targetRole;

    @Column(name = "target_institution_id")
    private UUID targetInstitutionId;

    @Column(name = "sent_by")
    private String sentBy;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt = LocalDateTime.now();

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "read_count")
    private Integer readCount = 0;

    @Column(columnDefinition = "jsonb")
    private String metadata;
}
