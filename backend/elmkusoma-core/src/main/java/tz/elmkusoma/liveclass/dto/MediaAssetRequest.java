package tz.elmkusoma.liveclass.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaAssetRequest {
    private String title;
    private String description;
    private String mediaType;
    private String fileUrl;
    private String thumbnailUrl;
    private Long durationSeconds;
    private Long fileSizeBytes;
    private String mimeType;
    private String status;
    private String visibility;
    private String sourceType;
    private UUID sourceId;
    private UUID courseId;
    private UUID subjectId;
    private String tags;
}
