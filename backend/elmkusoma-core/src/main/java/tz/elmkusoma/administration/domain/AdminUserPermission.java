package tz.elmkusoma.administration.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Per-account permission entry for the platform-admin matrix (audit B-10).
 *
 * <p>Previously the matrix was written into {@code role_permissions.role_id}, a column that
 * also stores {@code custom_roles.id}; a UUID collision could therefore overwrite a real
 * role's permissions. This table is keyed by {@code users.id} only, so the two key spaces
 * can never collide. The role-keyed table and its endpoints remain in use for custom roles.
 */
@Entity
@Table(name = "admin_user_permissions")
public class AdminUserPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "permission", nullable = false)
    private String permission;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean deleted) { this.isDeleted = deleted; }

    public static AdminUserPermission of(UUID userId, String permission, String actor) {
        AdminUserPermission p = new AdminUserPermission();
        p.userId = userId;
        p.permission = permission;
        p.createdBy = actor;
        return p;
    }
}