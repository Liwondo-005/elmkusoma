package tz.elmkusoma.learning.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "resource_analytics", uniqueConstraints = {
    @UniqueConstraint(name = "uk_resource_analytics_daily", columnNames = {"resource_id", "date"})
}, indexes = {
    @Index(name = "idx_analytics_resource", columnList = "resource_id"),
    @Index(name = "idx_analytics_date", columnList = "date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ResourceAnalytics extends BaseEntity {

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(name = "institution_id", nullable = false)
    private UUID institutionId;

    @Column(name = "date", nullable = false)
    private java.time.LocalDate date;

    @Column(name = "view_count")
    @Builder.Default
    private Integer viewCount = 0;

    @Column(name = "download_count")
    @Builder.Default
    private Integer downloadCount = 0;

    @Column(name = "unique_viewers")
    @Builder.Default
    private Integer uniqueViewers = 0;

    @Column(name = "unique_downloaders")
    @Builder.Default
    private Integer uniqueDownloaders = 0;

    @Column(name = "total_watch_time_seconds")
    @Builder.Default
    private Long totalWatchTimeSeconds = 0L;

    @Column(name = "avg_watch_time_seconds", precision = 10, scale = 2)
    @Builder.Default
    private java.math.BigDecimal avgWatchTimeSeconds = java.math.BigDecimal.ZERO;



    // Explicit getId for BaseEntity inheritance
    public UUID getId() { return getIdDirect(); }
    public void setId(UUID id) { setIdDirect(id); }

    // Helper methods
    public void incrementView() {
        this.viewCount = (this.viewCount != null ? this.viewCount : 0) + 1;
    }

    public void incrementDownload() {
        this.downloadCount = (this.downloadCount != null ? this.downloadCount : 0) + 1;
    }

    public void addWatchTime(int seconds) {
        this.totalWatchTimeSeconds = (this.totalWatchTimeSeconds != null ? this.totalWatchTimeSeconds : 0L) + seconds;
        if (this.viewCount != null && this.viewCount > 0) {
            this.avgWatchTimeSeconds = java.math.BigDecimal.valueOf(this.totalWatchTimeSeconds)
                    .divide(java.math.BigDecimal.valueOf(this.viewCount), 2, java.math.RoundingMode.HALF_UP);
        }
    }
}