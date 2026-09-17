package tz.elmkusoma.administration.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "dashboard_snapshots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "institution_id", nullable = false)
    private UUID institutionId;

    @Column(name = "snapshot_type", nullable = false)
    private String snapshotType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "snapshot_data", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> snapshotData;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public static DashboardSnapshot of(UUID institutionId, String snapshotType,
                                       Map<String, Object> snapshotData,
                                       LocalDateTime expiresAt) {
        DashboardSnapshot snapshot = new DashboardSnapshot();
        snapshot.setInstitutionId(institutionId);
        snapshot.setSnapshotType(snapshotType);
        snapshot.setSnapshotData(snapshotData);
        snapshot.setExpiresAt(expiresAt);
        return snapshot;
    }
}