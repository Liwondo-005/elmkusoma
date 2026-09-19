package tz.elmkusoma.nursery.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "nursery_stories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class NurseryStory extends BaseEntity {

    @Column(name = "class_group_id", nullable = false)
    private UUID classGroupId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "story_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private StoryType storyType;

    @Column(name = "illustration_url")
    private String illustrationUrl;

    @Column(name = "audio_url")
    private String audioUrl;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "reading_level")
    private String readingLevel;

    @Column(name = "is_published", nullable = false)
    private Boolean isPublished = false;

    public enum StoryType {
        ANIMAL, FAIRY_TALE, TANZANIA, EDUCATIONAL, ADVENTURE, MORAL
    }
}
