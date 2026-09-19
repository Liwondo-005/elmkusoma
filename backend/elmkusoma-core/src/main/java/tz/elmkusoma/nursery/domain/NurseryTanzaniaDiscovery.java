package tz.elmkusoma.nursery.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "nursery_tanzania_discovery")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class NurseryTanzaniaDiscovery extends BaseEntity {

    @Column(name = "class_group_id")
    private UUID classGroupId;

    @Column(name = "topic_title", nullable = false)
    private String topicTitle;

    @Column(name = "topic_description")
    private String topicDescription;

    @Column(name = "category", nullable = false)
    @Enumerated(EnumType.STRING)
    private DiscoveryCategory category;

    @Column(name = "region")
    private String region;

    @Column(name = "fun_facts", columnDefinition = "TEXT")
    private String funFacts;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "is_published", nullable = false)
    private Boolean isPublished = false;

    public enum DiscoveryCategory {
        ANIMALS, PLACES, CULTURE, FOOD, MUSIC, FLAGS, LANDMARKS, WEATHER
    }
}
