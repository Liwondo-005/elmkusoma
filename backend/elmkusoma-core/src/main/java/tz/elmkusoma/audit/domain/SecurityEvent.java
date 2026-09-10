package tz.elmkusoma.audit.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "security_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurityEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "institution_id")
    private UUID institutionId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "user_email")
    private String userEmail;

    @Column(name = "event_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private SecurityEventType eventType;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "location")
    private String location;

    @Column(name = "severity", nullable = false)
    @Enumerated(EnumType.STRING)
    private Severity severity = Severity.INFO;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "resolved", nullable = false)
    private Boolean resolved = false;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolved_by")
    private UUID resolvedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public enum SecurityEventType {
        LOGIN_SUCCESS,
        LOGIN_FAILURE,
        LOGOUT,
        PASSWORD_CHANGE,
        PASSWORD_RESET,
        ACCOUNT_LOCKED,
        PERMISSION_CHANGE,
        ROLE_CHANGE,
        SUSPICIOUS_ACTIVITY,
        DATA_EXPORT,
        DATA_BREACH_ATTEMPT,
        SESSION_EXPIRED,
        CERTIFICATE_REVOKED,
        CERTIFICATE_ISSUED
    }

    public enum Severity {
        INFO,
        WARNING,
        ERROR,
        CRITICAL,
        MEDIUM
    }
}
