package tz.elmkusoma.learning.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import tz.elmkusoma.common.BaseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "video_tutorial_progress", uniqueConstraints = {
    @UniqueConstraint(name = "uk_video_progress_student", columnNames = {"video_tutorial_id", "student_id"})
}, indexes = {
    @Index(name = "idx_video_progress_student", columnList = "student_id"),
    @Index(name = "idx_video_progress_tutorial", columnList = "video_tutorial_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class VideoTutorialProgress extends BaseEntity {

    @Column(name = "video_tutorial_id", nullable = false)
    private UUID videoTutorialId;

    @Column(name = "institution_id", nullable = false)
    private UUID institutionId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "position_seconds")
    @Builder.Default
    private Integer positionSeconds = 0;

    @Column(name = "completed")
    @Builder.Default
    private Boolean completed = false;

    @Column(name = "completion_percentage")
    @Builder.Default
    private Double completionPercentage = 0.00;

    @Column(name = "last_watched_at")
    private LocalDateTime lastWatchedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "watch_count")
    @Builder.Default
    private Integer watchCount = 0;

    @Column(name = "total_watch_time_seconds")
    @Builder.Default
    private Integer totalWatchTimeSeconds = 0;

    @Column(name = "last_position_seconds")
    @Builder.Default
    private Integer lastPositionSeconds = 0;



    // Explicit getId for BaseEntity inheritance
    public UUID getId() { return getIdDirect(); }
    public void setId(UUID id) { setIdDirect(id); }

    // Helper methods
    public void updateProgress(int positionSeconds, int durationSeconds) {
        this.positionSeconds = positionSeconds;
        this.lastPositionSeconds = positionSeconds;
        this.lastWatchedAt = LocalDateTime.now();
        this.watchCount = (this.watchCount != null ? this.watchCount : 0) + 1;
        
        if (durationSeconds > 0) {
            double percentage = (double) positionSeconds / durationSeconds * 100;
            this.completionPercentage = Math.min(100.0, Math.round(percentage * 100.0) / 100.0);
            if (percentage >= 95.0 && !Boolean.TRUE.equals(this.completed)) {
                this.completed = true;
                this.completedAt = LocalDateTime.now();
            }
        }
    }

    public void incrementWatchTime(int seconds) {
        this.totalWatchTimeSeconds = (this.totalWatchTimeSeconds != null ? this.totalWatchTimeSeconds : 0) + seconds;
    }
}