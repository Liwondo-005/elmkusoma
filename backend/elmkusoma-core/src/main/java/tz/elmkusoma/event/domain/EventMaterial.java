package tz.elmkusoma.event.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import tz.elmkusoma.common.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "event_materials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class EventMaterial extends BaseEntity {

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "material_type", nullable = false, length = 50)
    private String materialType;

    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = true;

    @Column(name = "uploaded_by")
    private UUID uploadedBy;

    public enum MaterialType {
        RECORDING, VIDEO, DOCUMENT, PRESENTATION, PDF, IMAGE, AUDIO, LINK, OTHER
    }
}
