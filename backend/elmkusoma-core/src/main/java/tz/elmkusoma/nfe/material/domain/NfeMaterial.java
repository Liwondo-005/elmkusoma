package tz.elmkusoma.nfe.material.domain;

import jakarta.persistence.*;
import lombok.*;
import tz.elmkusoma.common.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "nfe_materials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NfeMaterial extends BaseEntity {

    @Column(name = "provider_id", nullable = false)
    private UUID providerId;

    @Column(name = "program_id")
    private UUID programId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "material_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private MaterialType materialType;

    @Column(name = "content_url")
    private String contentUrl;

    @Builder.Default
    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Builder.Default
    @Column(name = "is_free", nullable = false)
    private Boolean isFree = false;

    public enum MaterialType {
        DOCUMENT, VIDEO, AUDIO, LINK, FILE
    }
}
