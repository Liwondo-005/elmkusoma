package tz.elmkusoma.primary.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import tz.elmkusoma.common.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "live_class_activities")
public class LiveClassActivity extends BaseEntity {

    @Column(name = "live_class_id", nullable = false)
    private UUID liveClassId;

    @Column(name = "created_by_id", nullable = false)
    private UUID createdById;

    @Column(name = "activity_type", nullable = false, length = 30)
    private String activityType;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "options", columnDefinition = "TEXT")
    private String options;

    @Column(name = "correct_answer", length = 500)
    private String correctAnswer;

    @Column(name = "is_published", nullable = false)
    private Boolean isPublished = false;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex = 0;

    public LiveClassActivity() {
    }

    public UUID getLiveClassId() {
        return liveClassId;
    }

    public void setLiveClassId(UUID liveClassId) {
        this.liveClassId = liveClassId;
    }

    public UUID getCreatedById() {
        return createdById;
    }

    public void setCreatedById(UUID createdById) {
        this.createdById = createdById;
    }

    public String getActivityType() {
        return activityType;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOptions() {
        return options;
    }

    public void setOptions(String options) {
        this.options = options;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public Boolean getIsPublished() {
        return isPublished;
    }

    public void setIsPublished(Boolean isPublished) {
        this.isPublished = isPublished;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }
}
