package tz.elmkusoma.liveclass.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaAssetResponse {
    private UUID id;
    private String title;
    private String description;
    private String mediaType;
    /**
     * Raw storage/CDN location as persisted. Deliberately {@code null} for
     * {@code sourceType = LIVE_CLASS} assets: a private-bucket object URL is not a playback
     * URL, and handing it to the browser is what produced Backblaze's {@code UnauthorizedAccess}
     * XML. Use {@link #playbackUrl} for those instead.
     */
    private String fileUrl;
    /**
     * Application-owned, per-request authorised playback path for LIVE_CLASS recordings.
     * {@code null} for every other asset type, whose ordinary {@link #fileUrl} behaviour is
     * preserved unchanged.
     */
    private String playbackUrl;
    private String thumbnailUrl;
    private Long durationSeconds;
    private Long fileSizeBytes;
    private String mimeType;
    private String status;
    private String visibility;
    private String sourceType;
    private UUID sourceId;
    private UUID teacherId;
    private UUID courseId;
    private UUID classGroupId;
    private UUID subjectId;
    private String tags;
    private LocalDateTime createdAt;
}
