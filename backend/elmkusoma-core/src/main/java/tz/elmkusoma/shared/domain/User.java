package tz.elmkusoma.shared.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.NaturalId;
import tz.elmkusoma.common.BaseEntity;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_email", columnList = "email", unique = true),
    @Index(name = "idx_users_institution", columnList = "institution_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @NaturalId
    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "password_reset_token")
    private String passwordResetToken;

    @Column(name = "password_reset_expires_at")
    private Instant passwordResetExpiresAt;

    public enum UserStatus {
        ACTIVE, INACTIVE, SUSPENDED, PENDING_VERIFICATION
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
